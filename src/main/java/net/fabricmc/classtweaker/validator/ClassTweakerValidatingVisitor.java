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

import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;
import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrMethod;

public class ClassTweakerValidatingVisitor implements ClassTweakerVisitor {
	private final TrEnvironment environment;

	public ClassTweakerValidatingVisitor(TrEnvironment environment) {
		this.environment = environment;
	}

	@Override
	public @Nullable AccessWidenerVisitor visitAccessWidener(String owner) {
		return new AccessWidenerValidatingVisitor(environment, owner);
	}

	@Override
	public @Nullable EnumExtensionVisitor visitEnum(String owner, String name, String constructorDesc, String id, boolean transitive) {
		final TrClass trClass = environment.getClass(owner);

		if (trClass == null) {
			throw new ClassTweakerValidationException("Could not find target class (%s)", owner);
		}

		final TrMethod trConstructor = trClass.getMethod("<init>", constructorDesc);

		if (trConstructor == null) {
			throw new ClassTweakerValidationException("Could not find target constructor (<init>%s) in class (%s)", constructorDesc, owner);
		}

		return new EnumExtensionValidatingVisitor(environment, owner, name, constructorDesc);
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		if (environment.getClass(owner) == null) {
			throw new ClassTweakerValidationException("Could not find target class (%s)", owner);
		}
	}
}
