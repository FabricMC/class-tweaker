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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectArrayAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

public class InterfaceInjectionClassVisitorTest extends ClassVisitorTest {
	@Test
	void testSimple() throws Exception {
		classTweaker.visitInjectedInterface("test/FinalClass", "test/InterfaceTests", false);
		Class<?> testClass = applyTransformer("test.FinalClass");
		assertThat(testClass.getInterfaces()).singleElement()
				.satisfies(itf -> assertThat(itf.getName()).isEqualTo("test.InterfaceTests"));
	}

	@Test
	void testSimpleInner() throws Exception {
		classTweaker.visitInjectedInterface("test/PrivateInnerClass$Inner", "test/InterfaceTests", false);
		Class<?> testClass = applyTransformer().get("test.PrivateInnerClass$Inner");
		assertThat(testClass.getInterfaces()).singleElement()
				.satisfies(itf -> assertThat(itf.getName()).isEqualTo("test.InterfaceTests"));
	}

	@Test
	void testGenericInterface() throws Exception {
		classTweaker.visitInjectedInterface("test/FinalClass", "test/GenericInterface<Ljava/lang/String;>", false);
		Class<?> testClass = applyTransformer("test.FinalClass");
		assertGenericType(assertThat(testClass.getGenericInterfaces()).singleElement(), "test.GenericInterface")
				.singleElement()
				.isEqualTo(String.class);
	}

	@Test
	void testPassingGenericInterface() throws Exception {
		classTweaker.visitInjectedInterface("test/GenericClass", "test/GenericInterface<TT;>", false);
		Class<?> testClass = applyTransformer("test.GenericClass");
		assertGenericType(assertThat(testClass.getGenericInterfaces()).singleElement(), "test.GenericInterface")
				.singleElement()
				.isInstanceOf(TypeVariable.class)
				.satisfies(typeVar -> assertThat(typeVar.getTypeName()).isEqualTo("T"));
	}

	@Test
	void testAdvancedGenericInterface() throws Exception {
		classTweaker.visitInjectedInterface("test/GenericClass", "test/AdvancedGenericInterface<Ljava/util/function/Predicate<TT;>;Ljava/lang/Integer;>", false);
		Class<?> testClass = applyTransformer("test.GenericClass");
		assertGenericType(assertThat(testClass.getGenericInterfaces()).singleElement(), "test.AdvancedGenericInterface")
				.satisfiesExactly(
						firstParam -> assertGenericType(assertThat(firstParam), "java.util.function.Predicate")
								.singleElement()
								.isInstanceOf(TypeVariable.class)
								.satisfies(typeVar -> assertThat(typeVar.getTypeName()).isEqualTo("T")),
						secondParam -> assertThat(secondParam).isEqualTo(Integer.class)
				);
	}

	@Test
	void testTwoInjectedInterfaces() throws Exception {
		classTweaker.visitInjectedInterface("test/GenericClass", "test/GenericInterface<TT;>", false);
		classTweaker.visitInjectedInterface("test/GenericClass", "test/GenericInterface2<Ljava/lang/Integer;>", false);
		Class<?> testClass = applyTransformer("test.GenericClass");
		assertThat(testClass.getGenericInterfaces()).satisfiesExactly(
				firstInterface -> assertGenericType(assertThat(firstInterface), "test.GenericInterface")
						.singleElement()
						.isInstanceOf(TypeVariable.class)
						.satisfies(typeVar -> assertThat(typeVar.getTypeName()).isEqualTo("T")),
				secondInterface -> assertGenericType(assertThat(secondInterface), "test.GenericInterface2")
						.singleElement()
						.isEqualTo(Integer.class)
		);
	}

	@Test
	void testInterfaceReferencesUnknownTypeVariable() {
		classTweaker.visitInjectedInterface("test/FinalClass", "test/GenericInterface<TT;>", false);
		assertThatThrownBy(() -> applyTransformer("test.GenericInterface"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Interface test/GenericInterface attempted to use a type variable named T which is not present in the test/FinalClass class");
	}

	private static ObjectArrayAssert<Type> assertGenericType(ObjectAssert<Type> type, String rawType) {
		return type.isInstanceOf(ParameterizedType.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ParameterizedType.class))
				.satisfies(parameterized -> assertThat(parameterized.getRawType().getTypeName()).isEqualTo(rawType))
				.extracting(ParameterizedType::getActualTypeArguments, InstanceOfAssertFactories.array(Type[].class));
	}
}
