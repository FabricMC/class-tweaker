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

import org.objectweb.asm.Type;

import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;
import net.fabricmc.tinyremapper.api.TrEnvironment;

public class EnumExtensionValidatingVisitor implements EnumExtensionVisitor {
	private final TrEnvironment environment;
	private final String owner;
	private final String name;
	private final Type constructorType;

	private boolean readParams = false;

	public EnumExtensionValidatingVisitor(TrEnvironment environment, String owner, String name, String constructorDesc) {
		this.environment = environment;
		this.owner = owner;
		this.name = name;
		this.constructorType = Type.getType(constructorDesc);
	}

	@Override
	public void visitParameterList(String owner, String name, String desc) {
		visitParameters();
	}

	@Override
	public void visitParameterConstants(Object[] constants) {
		visitParameters();
	}

	@Override
	public void visitOverride(String methodName, String owner, String name, String desc) {
	}

	private void visitParameters() {
		if (readParams) {
			throw new ClassTweakerValidationException("Enum parameters have already been visited");
		}

		if (constructorType.getArgumentTypes().length == 2) {
			throw new ClassTweakerValidationException("Did not expect parameters for enum constructor (%s)", constructorType.getDescriptor());
		}

		readParams = true;
	}

	@Override
	public void visitEnd() {
		if (constructorType.getArgumentTypes().length > 2 && !readParams) {
			throw new ClassTweakerValidationException("Expected parameters for enum constructor (%s)", constructorType.getDescriptor());
		}
	}
}
