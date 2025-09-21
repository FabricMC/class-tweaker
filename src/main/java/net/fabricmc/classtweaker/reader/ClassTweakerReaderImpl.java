/*
 * Copyright (c) 2020 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.classtweaker.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.ClassTweakerReader;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;

public final class ClassTweakerReaderImpl implements ClassTweakerReader {
	public static final Charset ENCODING = StandardCharsets.UTF_8;

	// Also includes some weirdness such as vertical tabs
	private static final Pattern V1_DELIMITER = Pattern.compile("\\s+");
	// Only spaces or tabs
	private static final Pattern V2_DELIMITER = Pattern.compile("[ \\t]+");
	// Prefix used on access types to denote the entry should be inherited by mods depending on this mod
	private static final String TRANSITIVE_PREFIX = "transitive-";

	private static final String ENUM_PARAMS_USAGE = "Expected (<tab> params <owner> <name> <desc>) got (%s)";
	private static final Pattern ENUM_PARAMS_STR_PATTERN = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

	private final ClassTweakerVisitor visitor;

	private int lineNumber;

	public ClassTweakerReaderImpl(ClassTweakerVisitor visitor) {
		this.visitor = visitor;
	}

	public static int readVersion(byte[] content) {
		return readHeader(content).version;
	}

	public static int readVersion(BufferedReader reader) throws IOException {
		return readHeader(reader).version;
	}

	@Override
	public void read(byte[] content, String id) {
		read(content, null, id);
	}

	@Override
	public void read(byte[] content, String currentNamespace, String id) {
		String strContent = new String(content, ENCODING);

		try {
			read(new BufferedReader(new StringReader(strContent)), currentNamespace, id);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void read(BufferedReader reader, String id) throws IOException {
		read(reader, null, id);
	}

	@Override
	public void read(BufferedReader reader, String currentNamespace, String id) throws IOException {
		HeaderImpl header = readHeader(reader);
		lineNumber = 1;
		visitor.visitLineNumber(1);

		int version = header.version;

		if (currentNamespace != null && !header.namespace.equals(currentNamespace)) {
			throw error("Namespace (%s) does not match current runtime namespace (%s)", header.namespace, currentNamespace);
		}

		visitor.visitHeader(header.namespace);

		String line;

		Pattern delimiter = version < ClassTweaker.AW_V2 ? V1_DELIMITER : V2_DELIMITER;

		while ((line = reader.readLine()) != null) {
			lineNumber++;
			visitor.visitLineNumber(lineNumber);

			line = handleComment(version, line);

			if (line.isEmpty()) {
				continue;
			}

			if (Character.isWhitespace(line.codePointAt(0))) {
				throw error("Leading whitespace is not allowed");
			}

			// Note that this trims trailing spaces. See the docs of split for details.
			List<String> tokens = Arrays.asList(delimiter.split(line));

			String firstToken = tokens.get(0);

			if (version >= ClassTweaker.CT_V1) {
				if (("inject-interface".equals(firstToken) || (TRANSITIVE_PREFIX + "inject-interface").equals(firstToken))) {
					if (tokens.size() != 3) {
						throw error("Expected (inject-interface <className> <interfaceName>) got (%s)", line);
					}

					visitor.visitInjectedInterface(tokens.get(1), tokens.get(2), tokens.get(0).startsWith(TRANSITIVE_PREFIX));

					continue;
				}
			}

			boolean transitive = false;

			if (version >= ClassTweaker.AW_V2) {
				// transitive access widener flag
				if (firstToken.startsWith(TRANSITIVE_PREFIX)) {
					firstToken = firstToken.substring(TRANSITIVE_PREFIX.length());
					transitive = true;
				}
			}

			AccessWidenerVisitor.AccessType access = readAccessType(firstToken);

			if (tokens.size() < 2) {
				throw error("Expected <class|field|method> following " + tokens.get(0));
			}

			switch (tokens.get(1)) {
			case "class":
				handleClass(line, tokens, transitive, access);
				break;
			case "field":
				handleField(line, tokens, transitive, access);
				break;
			case "method":
				handleMethod(line, tokens, transitive, access);
				break;
			default:
				throw error("Unsupported type: '" + tokens.get(1) + "'");
			}
		}
	}

	public static HeaderImpl readHeader(byte[] content) {
		String strContent = new String(content, ENCODING);

		try {
			return readHeader(new BufferedReader(new StringReader(strContent)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static HeaderImpl readHeader(BufferedReader reader) throws IOException {
		String headerLine = reader.readLine();
		String[] header = headerLine.split("\\s+");

		if (header.length != 3 || (!header[0].equals("accessWidener") && !header[0].equals("classTweaker"))) {
			throw new ClassTweakerFormatException(
					1,
					"Invalid access widener file header. Expected: 'classTweaker <version> <namespace>'"
			);
		}

		final boolean accessWidener = header[0].equals("accessWidener");
		int version;

		if (accessWidener) {
			switch (header[1]) {
			case "v1":
				version = ClassTweaker.AW_V1;
				break;
			case "v2":
				version = ClassTweaker.AW_V2;
				break;
			default:
				throw new ClassTweakerFormatException(
						1,
						"Unsupported access widener format: " + header[1]
				);
			}
		} else {
			switch (header[1]) {
			case "v1":
				version = ClassTweaker.CT_V1;
				break;
			default:
				throw new ClassTweakerFormatException(
						1,
						"Unsupported class tweaker format: " + header[1]
				);
			}
		}

		return new HeaderImpl(version, header[2]);
	}

	private void handleClass(String line, List<String> tokens, boolean transitive, AccessWidenerVisitor.AccessType access) {
		if (tokens.size() != 3) {
			throw error("Expected (<access> class <className>) got (%s)", line);
		}

		String name = tokens.get(2);
		validateClassName(name);

		try {
			visitor.visitAccessWidener(name).visitClass(access, transitive);
		} catch (Exception e) {
			throw error(e.toString());
		}
	}

	private void handleField(String line, List<String> tokens, boolean transitive, AccessWidenerVisitor.AccessType access) {
		if (tokens.size() != 5) {
			throw error("Expected (<access> field <className> <fieldName> <fieldDesc>) got (%s)", line);
		}

		String owner = tokens.get(2);
		String fieldName = tokens.get(3);
		String descriptor = tokens.get(4);

		validateClassName(owner);

		try {
			visitor.visitAccessWidener(owner).visitField(fieldName, descriptor, access, transitive);
		} catch (Exception e) {
			throw error(e.toString());
		}
	}

	private void handleMethod(String line, List<String> tokens, boolean transitive, AccessWidenerVisitor.AccessType access) {
		if (tokens.size() != 5) {
			throw error("Expected (<access> method <className> <methodName> <methodDesc>) got (%s)", line);
		}

		String owner = tokens.get(2);
		String methodName = tokens.get(3);
		String descriptor = tokens.get(4);

		validateClassName(owner);

		try {
			visitor.visitAccessWidener(owner).visitMethod(methodName, descriptor, access, transitive);
		} catch (Exception e) {
			throw error(e.toString());
		}
	}

	private String handleComment(int version, String line) {
		//Comment handling
		int commentPos = line.indexOf('#');

		if (commentPos >= 0) {
			line = line.substring(0, commentPos);

			// In V1, trimming led to leading whitespace being tolerated
			// The tailing whitespace is already stripped by the split below
			if (version <= ClassTweaker.AW_V1) {
				line = line.trim();
			}
		}

		return line;
	}

	private AccessWidenerVisitor.AccessType readAccessType(String access) {
		switch (access.toLowerCase(Locale.ROOT)) {
		case "accessible":
			return AccessWidenerVisitor.AccessType .ACCESSIBLE;
		case "extendable":
			return AccessWidenerVisitor.AccessType .EXTENDABLE;
		case "mutable":
			return AccessWidenerVisitor.AccessType .MUTABLE;
		default:
			throw error("Unknown access type: " + access);
		}
	}

	private ClassTweakerFormatException error(String format, Object... args) {
		// Note that getLineNumber is actually 1 line after the current line position,
		// because it is 0-based. But since our reporting here is 1-based, it works out.
		// If this class ever starts reading lines incrementally however, it'd need to be changed.
		String message = String.format(Locale.ROOT, format, args);
		return new ClassTweakerFormatException(lineNumber, message);
	}

	private void validateClassName(String className) {
		// Common mistake is using periods to separate packages/class names
		if (className.contains(".")) {
			throw error("Class-names must be specified as a/b/C, not a.b.C, but found: %s", className);
		}
	}

	static class HeaderImpl implements Header {
		private final int version;
		private final String namespace;

		HeaderImpl(int version, String namespace) {
			this.version = version;
			this.namespace = namespace;
		}

		@Override
		public int getVersion() {
			return version;
		}

		@Override
		public String getNamespace() {
			return namespace;
		}
	}
}
