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

import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.tinyremapper.api.TrEnvironment;

public class AccessWidenerValidatingVisitor implements AccessWidenerVisitor {
	private final TrEnvironment environment;
	private final String owner;

	public AccessWidenerValidatingVisitor(TrEnvironment environment, String owner) {
		this.environment = environment;
		this.owner = owner;
	}

	@Override
	public void visitClass(AccessWidenerVisitor.AccessType access, boolean transitive) {
		if (environment.getClass(owner) == null) {
			throw new ClassTweakerValidationException("Could not find class (%s)", owner);
		}
	}

	@Override
	public void visitMethod(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
		if (environment.getMethod(owner, name, descriptor) == null) {
			throw new ClassTweakerValidationException("Could not find method (%s%s) in class (%s)", name, descriptor, owner);
		}
	}

	@Override
	public void visitField(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
		if (environment.getField(owner, name, descriptor) == null) {
			throw new ClassTweakerValidationException("Could not find field (%s%s) in class (%s)", name, descriptor, owner);
		}
	}
}
