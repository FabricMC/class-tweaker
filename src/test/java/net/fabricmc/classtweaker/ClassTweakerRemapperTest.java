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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.commons.SimpleRemapper;

import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.ClassTweakerWriter;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;

class ClassTweakerRemapperTest {
	SimpleRemapper remapper;

	@BeforeEach
	void setUp() {
		Map<String, String> mappings = new HashMap<>();
		mappings.put("a/Class", "newa/NewClass");
		mappings.put("g/Class", "newg/NewClass");
		mappings.put("x/Class", "newx/NewClass");
		mappings.put("a/Class.someMethod()I", "otherMethod");
		mappings.put("g/Class.someField", "otherField");
		mappings.put("z/Interface", "newz/Interface");
		remapper = new SimpleRemapper(mappings);
	}

	@Test
	void testRemappingWithUnexpectedNamespace() {
		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.CT_V1);
		ClassTweakerVisitor awRemapper = ClassTweakerVisitor.remap(writer, this.remapper, "expected_namespace", "target");
		IllegalArgumentException e = assertThrows(
				IllegalArgumentException.class,
				() -> awRemapper.visitHeader("unexpected_namespace")
		);
		assertThat(e).hasMessageContaining("Cannot remap access widener from namespace 'unexpected_namespace'");
	}

	@Test
	void testRemapping() throws Exception {
		ClassTweakerWriter writer = ClassTweakerWriter.create(ClassTweaker.CT_V1);
		accept(ClassTweakerVisitor.remap(writer, remapper, "original_namespace", "different_namespace"));
		assertEquals(readReferenceContent("Remapped.txt"), writer.writeString());
	}

	void accept(ClassTweakerVisitor visitor) {
		visitor.visitHeader("original_namespace");
		visitor.visitAccessWidener("a/Class").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		visitor.visitAccessWidener("x/Class").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, false);
		visitor.visitAccessWidener("a/Class").visitMethod("someMethod", "()I", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		visitor.visitAccessWidener("g/Class").visitField("someField", "I", AccessWidenerVisitor.AccessType.MUTABLE, false);
	}

	private String readReferenceContent(String name) throws IOException, URISyntaxException {
		URL resource = Objects.requireNonNull(getClass().getResource(name));
		String expectedContent = new String(Files.readAllBytes(
				Paths.get(resource.toURI())
		));
		return expectedContent.replace("\r\n", "\n"); // Normalize line endings
	}
}
