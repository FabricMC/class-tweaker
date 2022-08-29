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

package net.fabricmc.classtweaker.visitors;

import org.objectweb.asm.commons.Remapper;

import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;

class EnumExtensionRemapperVisitor implements EnumExtensionVisitor {
	private final EnumExtensionVisitor delegate;
	private final Remapper remapper;
	private final String enumOwner;

	EnumExtensionRemapperVisitor(EnumExtensionVisitor delegate, Remapper remapper, String enumOwner) {
		this.delegate = delegate;
		this.remapper = remapper;
		this.enumOwner = enumOwner;
	}

	@Override
	public void visitParameterList(String owner, String name, String desc) {
		delegate.visitParameterList(remapper.map(owner), remapper.mapFieldName(owner, name, desc), remapper.mapDesc(desc));
	}

	@Override
	public void visitParameterConstants(Object[] constants) {
		delegate.visitParameterConstants(constants);
	}

	@Override
	public void visitOverride(String methodName, String owner, String name, String desc) {
		delegate.visitOverride(remapper.mapMethodName(enumOwner, methodName, desc), remapper.map(owner), remapper.mapMethodName(owner, name, desc), remapper.mapMethodDesc(desc));
	}

	@Override
	public void visitEnd() {
		delegate.visitEnd();
	}
}
