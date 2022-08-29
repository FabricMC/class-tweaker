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

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;

/**
 * Decorates a {@link ClassTweakerVisitor} with a {@link Remapper}
 * to remap names passing through the visitor if they come from a different namespace.
 */
public final class ClassTweakerRemapperVisitor implements ClassTweakerVisitor {
	private final ClassTweakerVisitor delegate;
	private final Remapper remapper;
	private final String fromNamespace;
	private final String toNamespace;

	/**
	 * @param delegate      The visitor to forward the remapped information to.
	 * @param remapper      Will be used to remap names found in the access widener.
	 * @param fromNamespace The expected namespace of the access widener being remapped. Remapping fails if the
	 *                      actual namespace is different.
	 * @param toNamespace   The namespace that the access widener will be remapped to.
	 */
	public ClassTweakerRemapperVisitor(
			ClassTweakerVisitor delegate,
			Remapper remapper,
			String fromNamespace,
			String toNamespace
	) {
		this.delegate = delegate;
		this.remapper = remapper;
		this.fromNamespace = fromNamespace;
		this.toNamespace = toNamespace;
	}

	@Override
	public void visitHeader(String namespace) {
		if (!this.fromNamespace.equals(namespace)) {
			throw new IllegalArgumentException("Cannot remap access widener from namespace '" + namespace + "'."
					+ " Expected: '" + this.fromNamespace + "'");
		}

		delegate.visitHeader(toNamespace);
	}

	@Override
	public @Nullable AccessWidenerVisitor visitAccessWidener(String owner) {
		final AccessWidenerVisitor delegateAccessWidenerVisitor = delegate.visitAccessWidener(remapper.map(owner));

		if (delegateAccessWidenerVisitor == null) {
			return null;
		}

		return new AccessWidenerRemapperVisitor(delegateAccessWidenerVisitor, remapper, owner);
	}

	@Override
	public EnumExtensionVisitor visitEnum(String owner, String name, String constructorDesc, String id, boolean transitive) {
		final EnumExtensionVisitor delegateEnumExtensionVisitor = delegate.visitEnum(remapper.map(owner), name, remapper.mapMethodDesc(constructorDesc), id, transitive);

		if (delegateEnumExtensionVisitor == null) {
			return null;
		}

		return new EnumExtensionRemapperVisitor(delegateEnumExtensionVisitor, remapper, owner);
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		delegate.visitInjectedInterface(remapper.map(owner), remapper.map(iface), transitive);
	}
}
