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

package net.fabricmc.classtweaker.writer;

import net.fabricmc.classtweaker.api.ClassTweakerWriter;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;

public final class ClassTweakerWriterImpl implements ClassTweakerVisitor, ClassTweakerWriter {
	private final StringBuilder builder = new StringBuilder();
	private final int version;
	private String namespace;

	/**
	 * Constructs a writer that writes an access widener in the given version.
	 * If features not supported by the version are used, an exception is thrown.
	 */
	public ClassTweakerWriterImpl(int version) {
		this.version = version;
	}

	@Override
	public void visitHeader(String namespace) {
		if (this.namespace == null) {
			String header = "accessWidener";
			int headerVersion = version;

			if (version >= 3) {
				header = "classTweaker";
				headerVersion -= 2;
			}

			builder.append(header)
					.append("\tv")
					.append(headerVersion)
					.append('\t')
					.append(namespace)
					.append('\n');
		} else if (!this.namespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot write different namespaces to the same file ("
					+ this.namespace + " != " + namespace + ")");
		}

		this.namespace = namespace;
	}

	@Override
	public AccessWidenerVisitor visitAccessWidener(String owner) {
		return new AccessWidenerVisitor() {
			@Override
			public void visitClass(AccessType access, boolean transitive) {
				writeAccess(access, transitive);
				builder.append("\tclass\t").append(owner).append('\n');
			}

			@Override
			public void visitMethod(String name, String descriptor, AccessType access, boolean transitive) {
				writeAccess(access, transitive);
				builder.append("\tmethod\t").append(owner).append('\t').append(name)
						.append('\t').append(descriptor).append('\n');
			}

			@Override
			public void visitField(String name, String descriptor, AccessType access, boolean transitive) {
				writeAccess(access, transitive);
				builder.append("\tfield\t").append(owner).append('\t').append(name)
						.append('\t').append(descriptor).append('\n');
			}
		};
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		if (version < 3) {
			throw new IllegalArgumentException("Cannot write interface injection rule in version " + version);
		}

		if (transitive) {
			builder.append("transitive-");
		}

		builder.append("inject-interface\t").append(owner).append("\t").append(iface).append('\n');
	}

	@Override
	public String writeString() {
		if (namespace == null) {
			throw new IllegalStateException("No namespace set. visitHeader wasn't called.");
		}

		return builder.toString();
	}

	private void writeAccess(AccessWidenerVisitor.AccessType access, boolean transitive) {
		if (transitive) {
			if (version < 2) {
				throw new IllegalStateException("Cannot write transitive rule in version " + version);
			}

			builder.append("transitive-");
		}

		builder.append(access);
	}
}
