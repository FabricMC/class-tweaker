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

package net.fabricmc.classtweaker.classvisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;
import net.fabricmc.classtweaker.impl.ClassTweakerImpl;

public class EnumExtensionClassVisitorTest extends ClassVisitorTest {
	@Test
	void testAddSimpleEnum() throws Exception {
		classTweaker.visitEnum("test/SimpleEnum", "THREE", "(Ljava/lang/String;I)V", "test", false);
		Class<?> testClass = applyTransformer("test.SimpleEnum");

		assertThat(testClass.isEnum()).isTrue();
		assertThat(testClass.getEnumConstants()).hasSize(3);

		Method valueOfMethod = testClass.getDeclaredMethod("valueOf", String.class);
		Object o = valueOfMethod.invoke(null, "THREE");
		assertThat(o).isNotNull();
	}

	@Test
	void testAddSimpleEnumParams() throws Exception {
		EnumExtensionVisitor enumExtensionVisitor = classTweaker.visitEnum("test/ParamEnum", "Z", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", false);
		enumExtensionVisitor.visitParameterList("net/fabricmc/classtweaker/EnumTestConstants", "ENUM_PARAMS", "Ljava/util/List;");

		Class<?> testClass = applyTransformer("test.ParamEnum");

		assertThat(testClass.isEnum()).isTrue();
		assertThat(testClass.getEnumConstants()).hasSize(8);

		Method valueOfMethod = testClass.getDeclaredMethod("valueOf", String.class);
		Object o = valueOfMethod.invoke(null, "Z");
		assertThat(o).isNotNull();
	}

	@Test
	void testAddEnumConstantParams() throws Exception {
		EnumExtensionVisitor enumExtensionVisitor = classTweaker.visitEnum("test/ParamEnum2", "ADDED", "(Ljava/lang/String;IZCBSIFJDLjava/lang/String;Ljava/lang/Object;)V", "test", false);
		enumExtensionVisitor.visitParameterConstants(new Object[]{true, 'h', (byte) 123, (short) 4567, 1234567, 12.0F, 9999L, 1.12D, "Hello World", null});

		Class<?> testClass = applyTransformer("test.ParamEnum2");

		assertThat(testClass.isEnum()).isTrue();
		assertThat(testClass.getEnumConstants()).hasSize(2);

		Method valueOfMethod = testClass.getDeclaredMethod("valueOf", String.class);
		Object o = valueOfMethod.invoke(null, "ADDED");
		assertThat(o).isNotNull();
	}

	@Test
	void testAddEnumOverride() throws Exception {
		EnumExtensionVisitor enumExtensionVisitor = classTweaker.visitEnum("test/ComplexEnum", "ADDED", "(Ljava/lang/String;ILjava/lang/String;)V", "test", false);
		enumExtensionVisitor.visitOverride("hello", "net/fabricmc/classtweaker/EnumTestConstants", "hello", "(I)Z");
		enumExtensionVisitor.visitParameterConstants(new Object[]{"Hello world!"});

		Class<?> testClass = applyTransformer("test.ComplexEnum");

		assertThat(testClass.isEnum()).isTrue();
		assertThat(testClass.getEnumConstants()).hasSize(2);

		Method valueOfMethod = testClass.getDeclaredMethod("valueOf", String.class);
		Object o = valueOfMethod.invoke(null, "ADDED");
		assertThat(o).isNotNull();
	}

	@Test
	void testAddEmptyEnum() throws Exception {
		classTweaker.visitEnum("test/EmptyEnum", "ONE", "(Ljava/lang/String;I)V", "test", false);
		Class<?> testClass = applyTransformer("test.EmptyEnum");

		assertThat(testClass.isEnum()).isTrue();
		assertThat(testClass.getEnumConstants()).hasSize(1);

		Method valueOfMethod = testClass.getDeclaredMethod("valueOf", String.class);
		Object o = valueOfMethod.invoke(null, "ONE");
		assertThat(o).isNotNull();
	}

	@Test
	void testReapplySame() {
		classTweaker.visitEnum("test/EmptyEnum", "ONE", "(Ljava/lang/String;I)V", "test", false);
		ClassNode classNode = readClass("test.EmptyEnum");

		classNode.accept(classTweaker.createClassVisitor(Opcodes.ASM9, classNode, null));
		classNode.accept(classTweaker.createClassVisitor(Opcodes.ASM9, classNode, null));
	}

	@Test
	void testReapplyDifferingId() {
		ClassTweakerImpl classTweaker2 = new ClassTweakerImpl();
		classTweaker.visitEnum("test/EmptyEnum", "ONE", "(Ljava/lang/String;I)V", "test", false);
		classTweaker2.visitEnum("test/EmptyEnum", "ONE", "(Ljava/lang/String;I)V", "modid", false);
		ClassNode classNode = readClass("test.EmptyEnum");

		classNode.accept(classTweaker.createClassVisitor(Opcodes.ASM9, classNode, null));
		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> classNode.accept(classTweaker2.createClassVisitor(Opcodes.ASM9, classNode, null))
		);
		assertThat(exception.getMessage()).isEqualTo("Enum addition from (modid) clashes with (test) in enum (test/EmptyEnum) for entry (ONE)");
	}

	@Test
	void testReapplyDifferingHash() {
		EnumExtensionVisitor enumExtensionVisitor = classTweaker.visitEnum("test/EmptyEnum", "ONE", "(Ljava/lang/String;I)V", "test", false);
		enumExtensionVisitor.visitParameterConstants(new Object[]{"Hello"});

		ClassTweakerImpl classTweaker2 = new ClassTweakerImpl();
		enumExtensionVisitor = classTweaker2.visitEnum("test/EmptyEnum", "ONE", "(Ljava/lang/String;I)V", "test", false);
		enumExtensionVisitor.visitParameterConstants(new Object[]{"Hello world"});

		ClassNode classNode = readClass("test.EmptyEnum");

		classNode.accept(classTweaker.createClassVisitor(Opcodes.ASM9, classNode, null));
		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> classNode.accept(classTweaker2.createClassVisitor(Opcodes.ASM9, classNode, null))
		);
		assertThat(exception.getMessage()).isEqualTo("Previously applied enum addition from (test) does not match");
	}
}
