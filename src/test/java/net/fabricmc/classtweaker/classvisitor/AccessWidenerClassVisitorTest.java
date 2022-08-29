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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.PrivateInnerClass;

import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;

class AccessWidenerClassVisitorTest extends ClassVisitorTest {
	@Nested
	class Classes {
		@Test
		void testMakeAccessible() throws Exception {
			classTweaker.visitAccessWidener("test/PackagePrivateClass").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.PackagePrivateClass");
			assertThat(testClass).isPublic();
		}

		@Test
		void testMakeExtendable() throws Exception {
			classTweaker.visitAccessWidener("test/PackagePrivateClass").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.PackagePrivateClass");
			assertThat(testClass).isPublic().isNotFinal();
		}

		@Test
		void testMakeAccessibleAndExtendable() throws Exception {
			classTweaker.visitAccessWidener("test/FinalPackagePrivateClass").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			classTweaker.visitAccessWidener("test/FinalPackagePrivateClass").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.FinalPackagePrivateClass");
			assertThat(testClass).isPublic().isNotFinal();
		}

		@Test
		void testMakeInnerClassAccessible() throws Exception {
			classTweaker.visitAccessWidener("test/PrivateInnerClass$Inner").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			Map<String, Class<?>> classes = applyTransformer();
			assertThat(classes).containsOnlyKeys("test.PrivateInnerClass$Inner", "test.PrivateInnerClass");

			Class<?> outerClass = classes.get("test.PrivateInnerClass");
			Class<?> innerClass = classes.get("test.PrivateInnerClass$Inner");
			assertThat(outerClass.getClasses()).containsOnly(innerClass);
			// For comparison purposes, the untransformed outer class has no public inner-classes
			assertThat(PrivateInnerClass.class.getClasses()).isEmpty();

			assertThat(innerClass).isPublic();
		}

		@Test
		void testMakeInnerClassExtendable() throws Exception {
			classTweaker.visitAccessWidener("test/FinalPrivateInnerClass$Inner").visitClass(AccessWidenerVisitor.AccessType.EXTENDABLE, false);
			Map<String, Class<?>> classes = applyTransformer();
			assertThat(classes).containsOnlyKeys("test.FinalPrivateInnerClass$Inner", "test.FinalPrivateInnerClass");

			Class<?> outerClass = classes.get("test.FinalPrivateInnerClass");
			Class<?> innerClass = classes.get("test.FinalPrivateInnerClass$Inner");
			assertThat(outerClass.getClasses()).containsOnly(innerClass);
			assertThat(innerClass).isPublic().isNotFinal();
		}
	}

	@Nested
	class Fields {
		@Test
		void testMakeAccessible() throws Exception {
			classTweaker.visitAccessWidener("test/FieldTests").visitField("privateFinalIntField", "I", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.FieldTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			assertEquals("public final", Modifier.toString(testClass.getDeclaredField("privateFinalIntField").getModifiers()));
		}

		@Test
		void testMakeMutable() throws Exception {
			classTweaker.visitAccessWidener("test/FieldTests").visitField("privateFinalIntField", "I", AccessWidenerVisitor.AccessType.MUTABLE, false);
			Class<?> testClass = applyTransformer("test.FieldTests");

			// making the field mutable does not affect the containing class
			assertEquals("final", Modifier.toString(testClass.getModifiers()));
			assertEquals("private", Modifier.toString(testClass.getDeclaredField("privateFinalIntField").getModifiers()));
		}

		@Test
		void testMakeMutableAndAccessible() throws Exception {
			classTweaker.visitAccessWidener("test/FieldTests").visitField("privateFinalIntField", "I", AccessWidenerVisitor.AccessType.MUTABLE, false);
			classTweaker.visitAccessWidener("test/FieldTests").visitField("privateFinalIntField", "I", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.FieldTests");

			// Making the field accessible and mutable affects the class visibility
			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			assertEquals("public", Modifier.toString(testClass.getDeclaredField("privateFinalIntField").getModifiers()));
		}

		@Test
		void testDontMakeInterfaceMutable() throws Exception {
			classTweaker.visitAccessWidener("test/InterfaceTests").visitField("staticFinalIntField", "I", AccessWidenerVisitor.AccessType.MUTABLE, false);
			Class<?> testClass = applyTransformer("test.InterfaceTests");

			assertEquals("public static final", Modifier.toString(testClass.getDeclaredField("staticFinalIntField").getModifiers()));
		}
	}

	@Nested
	class Methods {
		@Test
		void testMakeAccessible() throws Exception {
			classTweaker.visitAccessWidener("test/MethodTests").visitMethod("privateMethod", "()V", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			// Note that this also made the method final
			assertEquals("public final", Modifier.toString(testClass.getDeclaredMethod("privateMethod").getModifiers()));
		}

		@Test
		void testMakeConstructorAccessible() throws Exception {
			classTweaker.visitAccessWidener("test/MethodTests").visitMethod("<init>", "()V", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			// Note that this did _not_ make the ctor final since constructors cannot be overridden anyway.
			assertEquals("public", Modifier.toString(testClass.getDeclaredConstructor().getModifiers()));
		}

		@Test
		void testMakeStaticMethodAccessible() throws Exception {
			classTweaker.visitAccessWidener("test/MethodTests").visitMethod("staticMethod", "()V", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public final", Modifier.toString(testClass.getModifiers()));
			// Note that this did _not_ make the method final since static methods cannot be overridden anyway.
			assertEquals("public static", Modifier.toString(testClass.getDeclaredMethod("staticMethod").getModifiers()));
		}

		@Test
		void testMakeExtendable() throws Exception {
			classTweaker.visitAccessWidener("test/MethodTests").visitMethod("privateMethod", "()V", AccessWidenerVisitor.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public", Modifier.toString(testClass.getModifiers()));
			assertEquals("protected", Modifier.toString(testClass.getDeclaredMethod("privateMethod").getModifiers()));
		}

		@Test
		void testMakeAccessibleAndExtendable() throws Exception {
			classTweaker.visitAccessWidener("test/MethodTests").visitMethod("privateMethod", "()V", AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
			classTweaker.visitAccessWidener("test/MethodTests").visitMethod("privateMethod", "()V", AccessWidenerVisitor.AccessType.EXTENDABLE, false);
			Class<?> testClass = applyTransformer("test.MethodTests");

			assertEquals("public", Modifier.toString(testClass.getModifiers()));
			assertEquals("public", Modifier.toString(testClass.getDeclaredMethod("privateMethod").getModifiers()));
		}

		@Test
		void testPrivateMethodCallsAreRewrittenToInvokeVirtual() throws Exception {
			classTweaker.visitAccessWidener("test/PrivateMethodSubclassTest").visitMethod("test", "()I", AccessWidenerVisitor.AccessType.EXTENDABLE, false);
			// We need to ensure that the subclass goes through our hacky class-loader as well
			classTweaker.visitAccessWidener("test.PrivateMethodSubclassTest$Subclass");
			Class<?> testClass = applyTransformer().get("test.PrivateMethodSubclassTest");
			int result = (int) testClass.getMethod("callMethodOnSubclass").invoke(null);
			int resultWithLambda = (int) testClass.getMethod("callMethodWithLambdaOnSubclass").invoke(null);
			// this signifies that the INVOKESPECIAL instruction got rewritten to INVOKEVIRTUAL and the
			// method of the same name in the subclass was invoked.
			assertThat(result).isEqualTo(456);
			assertThat(resultWithLambda).isEqualTo(456);
		}
	}
}
