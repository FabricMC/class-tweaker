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

import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;

final class AccessWidenerRemapperVisitor implements AccessWidenerVisitor {
	private final AccessWidenerVisitor delegate;
	private final Remapper remapper;
	private final String owner;

	AccessWidenerRemapperVisitor(AccessWidenerVisitor delegate, Remapper remapper, String owner) {
		this.delegate = delegate;
		this.remapper = remapper;
		this.owner = owner;
	}

	@Override
	public void visitClass(AccessType access, boolean transitive) {
		delegate.visitClass(access, transitive);
	}

	@Override
	public void visitMethod(String name, String descriptor, AccessType access, boolean transitive) {
		delegate.visitMethod(
				remapper.mapMethodName(owner, name, descriptor),
				remapper.mapDesc(descriptor),
				access,
				transitive
		);
	}

	@Override
	public void visitField(String name, String descriptor, AccessType access, boolean transitive) {
		delegate.visitField(
				remapper.mapFieldName(owner, name, descriptor),
				remapper.mapDesc(descriptor),
				access,
				transitive
		);
	}
}
