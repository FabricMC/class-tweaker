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

import org.junit.jupiter.api.Test;

import net.fabricmc.classtweaker.api.AccessWidener;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.classvisitor.AccessWidenerClassVisitor;
import net.fabricmc.classtweaker.impl.ClassTweakerImpl;

public class ClassTweakerTest {
	ClassTweakerImpl widener = new ClassTweakerImpl();

	// When multiple files are merged into one AccessWidener instance, it should check that
	// all files use the same namespace.
	@Test
	public void testNamespaceConflictsAreChecked() {
		widener.visitHeader("namespace1");
		Exception e = assertThrows(Exception.class, () -> widener.visitHeader("namespace2"));
		assertEquals("Namespace mismatch, expected namespace1 got namespace2", e.getMessage());
	}

	/**
	 * Checks that the parents of inner classes are also added as targets, even if there's no access widener
	 * targeting them directly. This is needed for {@link AccessWidenerClassVisitor#visitInnerClass} to work.
	 */
	@Test
	void testParentClassesAreAddedAsTargetsForInnerClasses() {
		widener.visitAccessWidener("a/b/C$IC1$IC2").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		assertThat(widener.getTargets()).containsOnly(
				"a.b.C",
				"a.b.C$IC1",
				"a.b.C$IC1$IC2"
		);
		assertThat(widener.getClasses())
				.containsOnly("a/b/C$IC1$IC2");
		assertThat(widener.getAccessWidener("a/b/C$IC1$IC2").getClassAccess())
				.matches(AccessWidener.Access::isAccessible);
	}

	/**
	 * Tests that class-names are expected to be slash-separated binary names, which are transformed
	 * to period-separated names for the target list, while the actual access patterns
	 * are stored using the slash-separated names, as ASM uses them.
	 */
	@Test
	void testClassNameInterpretation() {
		widener.visitAccessWidener("a/b/C").visitClass(AccessWidenerVisitor.AccessType.ACCESSIBLE, false);
		assertThat(widener.getTargets()).containsOnly("a.b.C");
		assertThat(widener.getAccessWidener("a/b/C").getClassAccess())
				.matches(AccessWidener.Access::isAccessible);
	}
}
