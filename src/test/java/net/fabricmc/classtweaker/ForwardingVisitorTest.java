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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.ClassTweakerWriter;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;

class ForwardingVisitorTest {
	ClassTweakerWriter writer1 = ClassTweakerWriter.create(ClassTweaker.CT_V1);
	ClassTweakerWriter writer2 = ClassTweakerWriter.create(ClassTweaker.CT_V1);
	ClassTweakerVisitor visitor = ClassTweakerVisitor.forward(writer1, writer2);

	@Test
	void visitHeader() {
		visitor.visitHeader("special-namespace");
		assertEquals("classTweaker\tv1\tspecial-namespace\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitClass() {
		visitor.visitHeader("special-namespace");
		visitor.visitAccessWidener("class-name").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, true);
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
				+ "transitive-accessible\tclass\tclass-name\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitMethod() {
		visitor.visitHeader("special-namespace");
		visitor.visitAccessWidener("class-name").visitMethod("method-name", "method-desc", AccessWidenerVisitor.AccessType.ACCESSIBLE, true);
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
				+ "transitive-accessible\tmethod\tclass-name\tmethod-name\tmethod-desc\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitField() {
		visitor.visitHeader("special-namespace");
		visitor.visitAccessWidener("class-name").visitField("field-name", "field-desc", AccessWidenerVisitor.AccessType.ACCESSIBLE, true);
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
				+ "transitive-accessible\tfield\tclass-name\tfield-name\tfield-desc\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitEnum() {
		visitor.visitHeader("special-namespace");
		visitor.visitEnum("test/ParamEnum", "Z", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", false);
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
								+ "extend-enum\ttest/ParamEnum\tZ\t(Ljava/lang/String;ILjava/lang/String;I)V\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitEnumParameterList() {
		visitor.visitHeader("special-namespace");
		EnumExtensionVisitor enumExtensionVisitor = visitor.visitEnum("test/ParamEnum", "Z", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", false);
		enumExtensionVisitor.visitParameterList("net/fabricmc/classtweaker/EnumTestConstants", "ENUM_PARAMS", "Ljava/util/List;");
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
								+ "extend-enum\ttest/ParamEnum\tZ\t(Ljava/lang/String;ILjava/lang/String;I)V\n"
								+ "\tparams\tnet/fabricmc/classtweaker/EnumTestConstants\tENUM_PARAMS\tLjava/util/List;\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitEnumConstants() {
		visitor.visitHeader("special-namespace");
		EnumExtensionVisitor enumExtensionVisitor = visitor.visitEnum("test/ParamEnum", "Z", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", false);
		enumExtensionVisitor.visitParameterConstants(new Object[]{true, 'h', (byte) 123, (short) 4567, 1234567, 12.0F, 9999L, 1.12D, "Hello World", null});
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
								+ "extend-enum\ttest/ParamEnum\tZ\t(Ljava/lang/String;ILjava/lang/String;I)V\n"
								+ "\tparams\ttrue\th\t123\t4567\t1234567\t12.0\t9999\t1.12\t\"Hello World\"\tnull\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitEnumOverride() {
		visitor.visitHeader("special-namespace");
		EnumExtensionVisitor enumExtensionVisitor = visitor.visitEnum("test/ParamEnum", "Z", "(Ljava/lang/String;ILjava/lang/String;I)V", "test", false);
		enumExtensionVisitor.visitOverride("hello", "net/fabricmc/classtweaker/EnumTestConstants", "hello", "(I)Z");
		enumExtensionVisitor.visitParameterConstants(new Object[]{"Hello world!"});
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
								+ "extend-enum\ttest/ParamEnum\tZ\t(Ljava/lang/String;ILjava/lang/String;I)V\n"
								+ "\toverride\thello\tnet/fabricmc/classtweaker/EnumTestConstants\thello\t(I)Z\n"
								+ "\tparams\t\"Hello world!\"\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}

	@Test
	void visitInjectedInterface() {
		visitor.visitHeader("special-namespace");
		visitor.visitInjectedInterface("test/FinalClass", "test/InterfaceTests", false);
		assertEquals("classTweaker\tv1\tspecial-namespace\n"
								+ "inject-interface\ttest/FinalClass\ttest/InterfaceTests\n", writer1.writeString());
		assertEquals(writer1.writeString(), writer2.writeString());
	}
}
