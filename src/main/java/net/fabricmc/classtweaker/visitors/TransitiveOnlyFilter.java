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

import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;

/**
 * Decorates a visitor to only receive elements that are marked as transitive.
 */
public final class TransitiveOnlyFilter implements ClassTweakerVisitor {
	private final ClassTweakerVisitor delegate;

	public TransitiveOnlyFilter(ClassTweakerVisitor delegate) {
		this.delegate = delegate;
	}

	@Override
	public void visitHeader(String namespace) {
		delegate.visitHeader(namespace);
	}

	@Override
	public @Nullable AccessWidenerVisitor visitAccessWidener(String owner) {
		final AccessWidenerVisitor delegateAccessWidenerVisitor = delegate.visitAccessWidener(owner);

		if (delegateAccessWidenerVisitor != null) {
			return new AccessWidenerVisitor() {
				@Override
				public void visitClass(AccessType access, boolean transitive) {
					if (transitive) {
						delegateAccessWidenerVisitor.visitClass(access, transitive);
					}
				}

				@Override
				public void visitMethod(String name, String descriptor, AccessType access, boolean transitive) {
					if (transitive) {
						delegateAccessWidenerVisitor.visitMethod(name, descriptor, access, transitive);
					}
				}

				@Override
				public void visitField(String name, String descriptor, AccessType access, boolean transitive) {
					if (transitive) {
						delegateAccessWidenerVisitor.visitField(name, descriptor, access, transitive);
					}
				}
			};
		}

		return null;
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		if (transitive) {
			delegate.visitInjectedInterface(owner, iface, transitive);
		}
	}
}
