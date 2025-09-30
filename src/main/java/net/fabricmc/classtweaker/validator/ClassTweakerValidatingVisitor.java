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

package net.fabricmc.classtweaker.validator;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.classtweaker.api.ProblemSink;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.tinyremapper.api.TrEnvironment;

public class ClassTweakerValidatingVisitor implements ClassTweakerVisitor {
	private final TrEnvironment environment;
	private final ProblemSink sink;
	private int lineNumber;

	public ClassTweakerValidatingVisitor(TrEnvironment environment, ProblemSink sink) {
		this.environment = environment;
		this.sink = sink;
	}

	@Override
	public @Nullable AccessWidenerVisitor visitAccessWidener(String owner) {
		return new AccessWidenerValidatingVisitor(environment, sink, owner, lineNumber);
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		if (environment.getClass(owner) == null) {
			sink.addProblem(lineNumber, String.format("Could not find target class (%s)", owner));
		}
	}

	@Override
	public void visitLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
}
