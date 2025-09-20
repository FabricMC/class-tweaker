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

package net.fabricmc.classtweaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import net.fabricmc.classtweaker.api.AccessWidener;
import net.fabricmc.classtweaker.api.ClassTweakerReader;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.impl.AccessWidenerImpl;
import net.fabricmc.classtweaker.impl.ClassTweakerImpl;
import net.fabricmc.classtweaker.reader.ClassTweakerFormatException;
import net.fabricmc.classtweaker.utils.EntryTriple;

public class ClassTweakerReaderTest {
	ClassTweakerImpl visitor = new ClassTweakerImpl();

	ClassTweakerReader reader = ClassTweakerReader.create(visitor);

	@Nested
	class ReadVersion {
		@Test
		public void throwsOnInvalidFileHeader() {
			assertFormatError(
					"Invalid access widener file header. Expected: 'classTweaker <version> <namespace>'",
					() -> readVersion("accessWidenerX junk junk")
			);
		}

		@Test
		public void throwsOnUnsupportedVersion() {
			assertFormatError(
					"Unsupported access widener format: v99",
					() -> readVersion("accessWidener v99 junk")
			);
		}

		@Test
		public void readVersion1() {
			assertEquals(1, readVersion("accessWidener v1 junk"));
		}

		@Test
		public void readVersion2() {
			assertEquals(2, readVersion("accessWidener v2 junk"));
		}

		@Test
		public void readVersion3() {
			assertEquals(3, readVersion("classTweaker v1 junk"));
		}

		private int readVersion(String headerLine) {
			return ClassTweakerReader.readVersion(headerLine.getBytes(StandardCharsets.UTF_8));
		}
	}

	@Nested
	class Header {
		@Test
		public void throwsOnInvalidFileHeader() {
			assertFormatError(
					"Invalid access widener file header. Expected: 'classTweaker <version> <namespace>'",
					() -> parse("accessWidenerX junk junk\nxxx")
			);
		}

		@Test
		public void throwsOnUnsupportedVersion() {
			assertFormatError(
					"Unsupported access widener format: v99",
					() -> parse("accessWidener v99 junk\nxxx")
			);
		}

		@Test
		public void throwsOnUnsupportedClassTweakerVersion() {
			assertFormatError(
					"Unsupported class tweaker format: v99",
					() -> parse("classTweaker v99 junk")
			);
		}

		@Test
		public void throwsOnUnsupportedNamespaceIfNamespaceSet() {
			ClassTweakerFormatException e = assertThrows(
					ClassTweakerFormatException.class,
					() -> reader.read(new BufferedReader(new StringReader("accessWidener v1 junk\nxxx")), "expectedNamespace", "test")
			);
			assertThat(e).hasMessageContaining("Namespace (junk) does not match current runtime namespace (expectedNamespace)");
		}

		@Test
		public void acceptsMatchingNamespaceIfNamespaceSet() throws IOException {
			reader.read(new BufferedReader(new StringReader("accessWidener v1 expectedNamespace")), "expectedNamespace");
			assertEquals("expectedNamespace", visitor.getNamespace());
			assertEquals(Collections.emptySet(), visitor.getTargets());
		}

		@Test
		public void acceptsAnyNamespaceIfNoNamespaceSet() throws IOException {
			parse("accessWidener v1 anyWeirdNamespace");
			assertEquals("anyWeirdNamespace", visitor.getNamespace());
			assertEquals(Collections.emptySet(), visitor.getTargets());
		}

		@Test
		public void readHeader() {
			ClassTweakerReader.Header header = readHeader("accessWidener v2 named");
			assertEquals(2, header.getVersion());
			assertEquals("named", header.getNamespace());
		}

		private ClassTweakerReader.Header readHeader(String headerLine) {
			return ClassTweakerReader.readHeader(headerLine.getBytes(StandardCharsets.UTF_8));
		}
	}

	@Nested
	class Classes {
		@Test
		void testThrowsOnMissingTokensInLine() {
			assertFormatError(
					"Expected (<access> class <className>) got (accessible class)",
					() -> parseLines("accessible class")
			);
		}

		@Test
		void testThrowsOnExtraTokensInLine() {
			assertFormatError(
					"Expected (<access> class <className>) got (accessible class Class extra)",
					() -> parseLines("accessible class Class extra")
			);
		}

		@Test
		void testThrowsOnMutableClass() {
			assertFormatError(
					"java.lang.UnsupportedOperationException: Classes cannot be made mutable",
					() -> parseLines("mutable class Class")
			);
		}

		@Test
		public void testParseAccessible() throws IOException {
			testParseClassAccess(AccessWidenerImpl.ClassAccess.ACCESSIBLE, "accessible");
		}

		@Test
		public void testParseExtendable() throws IOException {
			testParseClassAccess(AccessWidenerImpl.ClassAccess.EXTENDABLE, "extendable");
		}

		@Test
		public void testParseAccessibleAndExtendable() throws IOException {
			// Test that they're merged the same way, independent of order
			testParseClassAccess(AccessWidenerImpl.ClassAccess.ACCESSIBLE_EXTENDABLE, "accessible", "extendable");
			testParseClassAccess(AccessWidenerImpl.ClassAccess.ACCESSIBLE_EXTENDABLE, "extendable", "accessible");
		}

		private void testParseClassAccess(AccessWidenerImpl.ClassAccess expectedAccess, String... keyword) throws IOException {
			String lines = Arrays.stream(keyword)
					.map(kw -> kw + " class some/test/Class")
					.collect(Collectors.joining("\n"));
			parseLines(lines);

			assertThat(visitor.getTargets()).containsOnly("some.test.Class");
			assertThat(getClasses()).containsOnly(entry("some/test/Class", expectedAccess));
			assertThat(getFields()).isEmpty();
			assertThat(getMethods()).isEmpty();
		}
	}

	@Nested
	class Fields {
		@Test
		void testThrowsOnMissingTokensInLine() {
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field)",
					() -> parseLines("accessible field")
			);
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field Class)",
					() -> parseLines("accessible field Class")
			);
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field Class Field)",
					() -> parseLines("accessible field Class Field")
			);
		}

		@Test
		void testThrowsOnExtraTokensInLine() {
			assertFormatError(
					"Expected (<access> field <className> <fieldName> <fieldDesc>) got (accessible field Class field I extra)",
					() -> parseLines("accessible field Class field I extra")
			);
		}

		@Test
		void testThrowsOnExtendableField() {
			assertFormatError(
					"java.lang.UnsupportedOperationException: Fields cannot be made extendable",
					() -> parseLines("extendable field Class field I")
			);
		}

		@Test
		public void testParseAccessible() throws IOException {
			testParseFieldAccess(
					AccessWidenerImpl.ClassAccess.ACCESSIBLE,
					AccessWidenerImpl.FieldAccess.ACCESSIBLE,
					"accessible"
			);
		}

		@Test
		public void testParseMutable() throws IOException {
			testParseFieldAccess(
					null,
					AccessWidenerImpl.FieldAccess.MUTABLE,
					"mutable"
			);
		}

		@Test
		public void testParseAccessibleAndMutable() throws IOException {
			// Test that they're merged the same way, independent of order
			testParseFieldAccess(
					AccessWidenerImpl.ClassAccess.ACCESSIBLE,
					AccessWidenerImpl.FieldAccess.ACCESSIBLE_MUTABLE,
					"accessible", "mutable"
			);
			testParseFieldAccess(
					AccessWidenerImpl.ClassAccess.ACCESSIBLE,
					AccessWidenerImpl.FieldAccess.ACCESSIBLE_MUTABLE,
					"mutable", "accessible"
			);
		}

		private void testParseFieldAccess(
				AccessWidenerImpl.ClassAccess expectedClassAccess,
				AccessWidenerImpl.FieldAccess expectedFieldAccess,
				String... keyword
		) throws IOException {
			String lines = Arrays.stream(keyword)
					.map(kw -> kw + " field some/test/Class someField I")
					.collect(Collectors.joining("\n"));
			parseLines(lines);

			assertThat(visitor.getTargets()).containsOnly("some.test.Class");

			if (expectedClassAccess != null) {
				assertThat(getClasses()).containsOnly(
						entry("some/test/Class", expectedClassAccess)
				);
			} else {
				assertThat(getClasses()).isEmpty();
			}

			assertThat(getFields()).containsOnly(
					entry(new EntryTriple("some/test/Class", "someField", "I"), expectedFieldAccess)
			);
			assertThat(getMethods()).isEmpty();
		}
	}

	@Nested
	class Methods {
		@Test
		void testThrowsOnMissingTokensInLine() {
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method)",
					() -> parseLines("accessible method")
			);
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method Method)",
					() -> parseLines("accessible method Method")
			);
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method Class Method)",
					() -> parseLines("accessible method Class Method")
			);
		}

		@Test
		void testThrowsOnExtraTokensInLine() {
			assertFormatError(
					"Expected (<access> method <className> <methodName> <methodDesc>) got (accessible method Method method ()V extra)",
					() -> parseLines("accessible method Method method ()V extra")
			);
		}

		@Test
		void testThrowsOnMutableMethod() {
			assertFormatError(
					"java.lang.UnsupportedOperationException: Methods cannot be made mutable",
					() -> parseLines("mutable method Class method ()V")
			);
		}

		@Test
		public void testParseAccessible() throws IOException {
			testParseMethodAccess(
					AccessWidenerImpl.ClassAccess.ACCESSIBLE,
					AccessWidenerImpl.MethodAccess.ACCESSIBLE,
					"accessible"
			);
		}

		@Test
		public void testParseExtendable() throws IOException {
			// Note how the class is also made implicitly extendable
			testParseMethodAccess(
					AccessWidenerImpl.ClassAccess.EXTENDABLE,
					AccessWidenerImpl.MethodAccess.EXTENDABLE,
					"extendable"
			);
		}

		@Test
		public void testParseAccessibleAndExtendable() throws IOException {
			// Test that they're merged the same way, independent of order
			testParseMethodAccess(
					AccessWidenerImpl.ClassAccess.ACCESSIBLE_EXTENDABLE,
					AccessWidenerImpl.MethodAccess.ACCESSIBLE_EXTENDABLE,
					"accessible", "extendable"
			);
			testParseMethodAccess(
					AccessWidenerImpl.ClassAccess.ACCESSIBLE_EXTENDABLE,
					AccessWidenerImpl.MethodAccess.ACCESSIBLE_EXTENDABLE,
					"extendable", "accessible"
			);
		}

		private void testParseMethodAccess(
				AccessWidenerImpl.ClassAccess expectedClassAccess,
				AccessWidenerImpl.MethodAccess expectedMethodAccess,
				String... keyword
		) throws IOException {
			String lines = Arrays.stream(keyword)
					.map(kw -> kw + " method some/test/Class someMethod ()V")
					.collect(Collectors.joining("\n"));
			parseLines(lines);

			assertThat(visitor.getTargets()).containsOnly("some.test.Class");

			if (expectedClassAccess != null) {
				assertThat(getClasses()).containsOnly(
						entry("some/test/Class", expectedClassAccess)
				);
			} else {
				assertThat(getClasses()).isEmpty();
			}

			assertThat(getMethods()).containsOnly(
					entry(new EntryTriple("some/test/Class", "someMethod", "()V"), expectedMethodAccess)
			);
			assertThat(getFields()).isEmpty();
		}
	}

	@Nested
	class GeneralParsing {
		@Test
		public void testCorrectLineNumbersInPresenceOfComments() {
			int lineNumber = assertThrows(ClassTweakerFormatException.class,
											() -> reader.read(new BufferedReader(new StringReader("accessWidener v1 namespace\n\n# comment\n\nERROR")), "test")
			).getLineNumber();
			assertEquals(5, lineNumber);
		}

		@Test
		public void throwsOnUnknownAccessType() {
			assertFormatError(
					"Unknown access type: somecommand",
					() -> parseLines("somecommand")
			);
		}

		@Test
		public void throwsOnMissingTypeAfterAccessible() {
			assertFormatError(
					"Expected <class|field|method> following accessible",
					() -> parseLines("accessible")
			);
		}

		@Test
		public void throwsOnInvalidTypeAfterAccessible() {
			assertFormatError(
					"Unsupported type: 'blergh'",
					() -> parseLines("accessible blergh")
			);
		}

		@Test
		public void throwsWithLeadingWhitespace() {
			assertFormatError(
					"Leading whitespace is not allowed",
					() -> parseLines("   accessible class SomeClass")
			);
		}

		// This is a quirk in access-widener v1
		@Test
		public void testLeadingWhitespaceWithLineComment() throws IOException {
			parseLines("   accessible class SomeClass #linecomment");
			assertThat(visitor.getTargets()).containsOnly("SomeClass");
		}

		@Test
		public void testTrailingWhitespace() throws IOException {
			parseLines("accessible class SomeClass    ");
			assertThat(visitor.getTargets()).containsOnly("SomeClass");
		}

		@Test
		public void testCanParseWithTabSeparators() throws IOException {
			parseLines("accessible\tclass\tSomeName");
			assertThat(visitor.getTargets()).containsOnly("SomeName");
		}

		@Test
		public void testCanParseWithMultipleSeparators() throws IOException {
			parseLines("accessible \tclass\t\t SomeName");
			assertThat(visitor.getTargets()).containsOnly("SomeName");
		}
	}

	@Nested
	class ClassNameValidation {
		@Test
		void testClassName() {
			assertFormatError(
					"Class-names must be specified as a/b/C, not a.b.C, but found: some.Class",
					() -> parseLines("accessible class some.Class")
			);
		}

		@Test
		void testClassNameInMethodWidener() {
			assertFormatError(
					"Class-names must be specified as a/b/C, not a.b.C, but found: some.Class",
					() -> parseLines("accessible method some.Class method ()V")
			);
		}

		@Test
		void testClassNameInFieldWidener() {
			assertFormatError(
					"Class-names must be specified as a/b/C, not a.b.C, but found: some.Class",
					() -> parseLines("accessible field some.Class field I")
			);
		}
	}

	/**
	 * Tests parsing features introduced in the V2 format.
	 */
	@Nested
	class V2Parsing {
		@Test
		void transitiveKeywordIsIgnoredWhenNoFilterIsSet() throws Exception {
			String testInput = readTestInput("AccessWidenerReaderTest_transitive.txt");
			parse(testInput);

			assertWidenerContains("local");
			assertWidenerContains("transitive");
			assertThat(getClasses()).hasSize(6);
			assertThat(getMethods()).hasSize(6);
			assertThat(getFields()).hasSize(4);
		}

		@Test
		void nonTransitiveEntriesAreIgnoredByNonTransitiveFilter() throws Exception {
			String testInput = readTestInput("AccessWidenerReaderTest_transitive.txt");
			reader = ClassTweakerReader.create(ClassTweakerVisitor.transitiveOnly(visitor));
			parse(testInput);

			assertWidenerContains("transitive");
			assertThat(getClasses()).hasSize(3);
			assertThat(getMethods()).hasSize(3);
			assertThat(getFields()).hasSize(2);
		}

		private void assertWidenerContains(String prefix) {
			assertThat(getClasses()).contains(
					entry(prefix + "/AccessibleClass", AccessWidenerImpl.ClassAccess.ACCESSIBLE),
					entry(prefix + "/ExtendableClass", AccessWidenerImpl.ClassAccess.EXTENDABLE),
					entry(prefix + "/AccessibleExtendableClass", AccessWidenerImpl.ClassAccess.ACCESSIBLE_EXTENDABLE)
			);
			assertThat(getMethods()).contains(
					entry(new EntryTriple(prefix + "/AccessibleClass", "method", "()V"), AccessWidenerImpl.MethodAccess.ACCESSIBLE),
					entry(new EntryTriple(prefix + "/ExtendableClass", "method", "()V"), AccessWidenerImpl.MethodAccess.EXTENDABLE),
					entry(new EntryTriple(prefix + "/AccessibleExtendableClass", "method", "()V"), AccessWidenerImpl.MethodAccess.ACCESSIBLE_EXTENDABLE)
			);
			assertThat(getFields()).contains(
					entry(new EntryTriple(prefix + "/AccessibleClass", "finalField", "I"), AccessWidenerImpl.FieldAccess.MUTABLE),
					entry(new EntryTriple(prefix + "/AccessibleClass", "field", "I"), AccessWidenerImpl.FieldAccess.ACCESSIBLE)
			);
		}
	}

	@Nested
	class V3Parsing {
		@Test
		void readInjectedInterface() throws Exception {
			String testInput = readTestInput("AccessWidenerReaderTest_interface.txt");
			parse(testInput);

			assertThat(visitor.getTargets()).hasSize(1);
		}
	}

	Map<String, AccessWidenerImpl.ClassAccess> getClasses() {
		Map<String, AccessWidenerImpl.ClassAccess> classes = new HashMap<>();

		for (String className : visitor.getClasses()) {
			AccessWidener.Access access = visitor.getAccessWidener(className).getClassAccess();

			if (access.isChanged()) {
				classes.put(className, (AccessWidenerImpl.ClassAccess) access);
			}
		}

		return classes;
	}

	Map<EntryTriple, AccessWidenerImpl.MethodAccess> getMethods() {
		Map<EntryTriple, AccessWidener.Access> methods = new HashMap<>();

		for (AccessWidener value : visitor.getAllAccessWideners().values()) {
			methods.putAll(value.getAllMethodAccesses());
		}

		//noinspection unchecked
		return (Map) methods;
	}

	Map<EntryTriple, AccessWidenerImpl.FieldAccess> getFields() {
		Map<EntryTriple, AccessWidener.Access> fields = new HashMap<>();

		for (AccessWidener value : visitor.getAllAccessWideners().values()) {
			fields.putAll(value.getAllFieldAccesses());
		}

		//noinspection unchecked
		return (Map) fields;
	}

	private void parse(String content) throws IOException {
		reader.read(new BufferedReader(new StringReader(content)), "test");
	}

	private void parseLines(String line) throws IOException {
		parse("accessWidener v1 namespace\n" + line);
	}

	private void assertFormatError(String expectedError, Executable executable) {
		ClassTweakerFormatException e = assertThrows(
				ClassTweakerFormatException.class,
				executable
		);
		assertEquals(expectedError, e.getMessage());
	}

	private String readTestInput(String name) throws Exception {
		URL resource = Objects.requireNonNull(getClass().getResource(name));
		return new String(Files.readAllBytes(
				Paths.get(resource.toURI())
		));
	}
}
