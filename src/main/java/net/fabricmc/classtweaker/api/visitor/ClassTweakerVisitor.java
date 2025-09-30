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

package net.fabricmc.classtweaker.api.visitor;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.classtweaker.api.ProblemSink;
import net.fabricmc.classtweaker.validator.ClassTweakerValidatingVisitor;
import net.fabricmc.classtweaker.visitors.ClassTweakerRemapperVisitor;
import net.fabricmc.classtweaker.visitors.ForwardingVisitor;
import net.fabricmc.classtweaker.visitors.TransitiveOnlyFilter;
import net.fabricmc.tinyremapper.api.TrEnvironment;

/**
 * A visitor of the entries defined in an access widener file.
 */
public interface ClassTweakerVisitor {
	/**
	 * Visits the header data.
	 *
	 * @param namespace the access widener's mapping namespace
	 */
	default void visitHeader(String namespace) {
	}

	@Nullable
	default AccessWidenerVisitor visitAccessWidener(String owner) {
		return null;
	}

	/**
	 * Visit an injected interface.
	 *
	 * @param owner the class name of the class to inject the interface onto
	 * @param iface the class name of the interface to inject onto the target class, plus any generics. If generics are
	 *              present, then this follows the format of
	 *              <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-ClassTypeSignature">JVMS 4.7.9.1 {@code ClassTypeSignature}s</a>,
	 *              minus the surrounding {@code L} and semicolon.
	 * @param transitive whether this widener should be applied across mod boundaries
	 */
	default void visitInjectedInterface(String owner, String iface, boolean transitive) {
	}

	/**
	 * Called by the reader to indicate that we are now on the given line number. Useful for reporting errors.
	 */
	default void visitLineNumber(int lineNumber) {
	}

	static ClassTweakerVisitor remap(ClassTweakerVisitor delegate,
			Remapper remapper,
			String fromNamespace,
			String toNamespace) {
		return new ClassTweakerRemapperVisitor(delegate, remapper, fromNamespace, toNamespace);
	}

	static ClassTweakerVisitor forward(ClassTweakerVisitor... visitors) {
		return new ForwardingVisitor(visitors);
	}

	static ClassTweakerVisitor transitiveOnly(ClassTweakerVisitor delegate) {
		return new TransitiveOnlyFilter(delegate);
	}

	static ClassTweakerVisitor validate(TrEnvironment environment, ProblemSink sink) {
		return new ClassTweakerValidatingVisitor(environment, sink);
	}
}
