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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.ClassTweakerWriter;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;

class ClassTweakerWriterTest {
	@Test
	void testCantWriteWithoutNamespace() {
		IllegalStateException e = assertThrows(IllegalStateException.class, ClassTweakerWriter.create(ClassTweaker.CT_V1)::writeString);
		assertThat(e).hasMessageContaining("No namespace set");
	}

	@Test
	void testWriteWidenerV1() throws Exception {
		String expectedContent = readReferenceContent("AccessWidenerWriterTest_v1.txt");

		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.AW_V1);
		accept(writer, 1);

		assertEquals(expectedContent, writer.writeString());
	}

	@Test
	void testWriteWidenerV2() throws Exception {
		String expectedContent = readReferenceContent("AccessWidenerWriterTest_v2.txt");

		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.AW_V2);
		accept(writer, 2);

		assertEquals(expectedContent, writer.writeString());
	}

	@Test
	void testWriteWidenerV3() throws Exception {
		String expectedContent = readReferenceContent("AccessWidenerWriterTest_v3.txt");

		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.CT_V1);
		accept(writer, 3);

		assertEquals(expectedContent, writer.writeString());
	}

	@Test
	void testCanMergeMultipleRunsIntoOneFile() {
		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.AW_V2);
		writer.visitHeader("ns1");
		writer.visitAccessWidener("SomeClass").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		writer.visitHeader("ns1");
		writer.visitAccessWidener("SomeClass").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, false);
		assertEquals("accessWidener\tv2\tns1\n"
				+ "accessible\tclass\tSomeClass\n"
				+ "extendable\tclass\tSomeClass\n", writer.writeString());
	}

	@Test
	void testDoesNotAllowDifferentNamespacesWhenMerging() {
		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.CT_V1);
		writer.visitHeader("ns1");
		assertThrows(Exception.class, () -> writer.visitHeader("ns2"));
	}

	@Test
	void testRejectsWritingV2FeaturesInV1Version() {
		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.AW_V1);
		writer.visitHeader("ns1");
		Exception e = assertThrows(Exception.class, () -> writer.visitAccessWidener("name").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, true));
		assertThat(e).hasMessageContaining("Cannot write transitive rule in version 1");
	}

	@Test
	void testRejectsWritingV3FeaturesInV2Version() {
		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.AW_V2);
		writer.visitHeader("ns1");
		Exception e = assertThrows(Exception.class, () -> writer.visitEnum("test/ParamEnum", "Z", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", false));
		assertThat(e).hasMessageContaining("Cannot write enum extension rule in version 2");
	}

	private String readReferenceContent(String name) throws IOException, URISyntaxException {
		URL resource = Objects.requireNonNull(getClass().getResource(name));
		String expectedContent = new String(Files.readAllBytes(
				Paths.get(resource.toURI())
		));
		return expectedContent.replace("\r\n", "\n"); // Normalize line endings
	}

	private void accept(ClassTweakerVisitor visitor, int version) {
		visitor.visitHeader("somenamespace");

		visitor.visitAccessWidener("pkg/AccessibleClass").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		visitor.visitAccessWidener("pkg/ExtendableClass").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, false);
		visitor.visitAccessWidener("pkg/AccessibleExtendableClass").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		visitor.visitAccessWidener("pkg/AccessibleExtendableClass").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, false);

		visitor.visitAccessWidener("pkg/AccessibleClass").visitMethod("method", "()V", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		visitor.visitAccessWidener("pkg/ExtendableClass").visitMethod("method", "()V", AccessWidenerVisitor.AccessType.EXTENDABLE, false);
		visitor.visitAccessWidener("pkg/AccessibleExtendableClass").visitMethod("method", "()V", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		visitor.visitAccessWidener("pkg/AccessibleExtendableClass").visitMethod("method", "()V", AccessWidenerVisitor.AccessType.EXTENDABLE, false);

		visitor.visitAccessWidener("pkg/AccessibleClass").visitField("field", "I", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		visitor.visitAccessWidener("pkg/AccessibleClass").visitField("finalField", "I", AccessWidenerVisitor.AccessType.MUTABLE, false);

		if (version >= 2) {
			final AccessWidenerVisitor accessWidenerVisitor = visitor.visitAccessWidener("pkg/TransitiveAccessibleClass");
			accessWidenerVisitor.visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, true);
			accessWidenerVisitor.visitMethod("method", "()V", AccessWidenerVisitor.AccessType.ACCESSIBLE, true);
			accessWidenerVisitor.visitField("field", "I", AccessWidenerVisitor.AccessType.ACCESSIBLE, true);
		}

		if (version >= 3) {
			visitor.visitEnum("test/SimpleEnum", "THREE", "(Ljava/lang/String;I)V", "test", false);

			EnumExtensionVisitor enumExtensionVisitor = visitor.visitEnum("test/ParamEnum", "Z", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", false);
			enumExtensionVisitor.visitParameterList("net/fabricmc/classtweaker/EnumTestConstants", "ENUM_PARAMS", "Ljava/util/List;");

			enumExtensionVisitor = visitor.visitEnum("test/ParamEnum", "X", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", true);
			enumExtensionVisitor.visitParameterConstants(new Object[]{"z", 1000});

			enumExtensionVisitor = visitor.visitEnum("test/ComplexEnum", "ADDED", "(Ljava/lang/String;ILjava/lang/String;)V", "test", false);
			enumExtensionVisitor.visitOverride("hello", "net/fabricmc/classtweaker/EnumTestConstants", "hello", "(I)Z");
			enumExtensionVisitor.visitParameterConstants(new Object[]{"Hello world!"});

			visitor.visitInjectedInterface("test/FinalClass", "test/InterfaceTests", false);
			visitor.visitInjectedInterface("test/FinalClass", "test/InterfaceTests", true);
		}
	}
}
