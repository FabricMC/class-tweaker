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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;

/**
 * Forwards visitor events to multiple other visitors.
 */
public class ForwardingVisitor implements ClassTweakerVisitor {
	private final ClassTweakerVisitor[] visitors;

	public ForwardingVisitor(ClassTweakerVisitor... visitors) {
		this.visitors = visitors.clone();
	}

	@Override
	public void visitHeader(String namespace) {
		for (ClassTweakerVisitor visitor : visitors) {
			visitor.visitHeader(namespace);
		}
	}

	@Override
	public @Nullable AccessWidenerVisitor visitAccessWidener(String owner) {
		List<AccessWidenerVisitor> accessWidenerVisitors = new ArrayList<>(visitors.length);

		for (ClassTweakerVisitor visitor : visitors) {
			accessWidenerVisitors.add(visitor.visitAccessWidener(owner));
		}

		return new ForwardingAccessWidenerVisitor(accessWidenerVisitors.toArray(new AccessWidenerVisitor[0]));
	}

	@Override
	public EnumExtensionVisitor visitEnum(String owner, String name, String constructorDesc, String id, boolean transitive) {
		List<EnumExtensionVisitor> enumExtensionVisitors = new ArrayList<>(visitors.length);

		for (ClassTweakerVisitor visitor : visitors) {
			enumExtensionVisitors.add(visitor.visitEnum(owner, name, constructorDesc, id, transitive));
		}

		return new ForwardingEnumExtensionVisitor(enumExtensionVisitors.toArray(new EnumExtensionVisitor[0]));
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		for (ClassTweakerVisitor visitor : visitors) {
			visitor.visitInjectedInterface(owner, iface, transitive);
		}
	}

	private static class ForwardingAccessWidenerVisitor implements AccessWidenerVisitor {
		private final AccessWidenerVisitor[] visitors;

		ForwardingAccessWidenerVisitor(AccessWidenerVisitor[] visitors) {
			this.visitors = visitors;
		}

		@Override
		public void visitClass(AccessType access, boolean transitive) {
			for (AccessWidenerVisitor visitor : visitors) {
				visitor.visitClass(access, transitive);
			}
		}

		@Override
		public void visitMethod(String name, String descriptor, AccessType access, boolean transitive) {
			for (AccessWidenerVisitor visitor : visitors) {
				visitor.visitMethod(name, descriptor, access, transitive);
			}
		}

		@Override
		public void visitField(String name, String descriptor, AccessType access, boolean transitive) {
			for (AccessWidenerVisitor visitor : visitors) {
				visitor.visitField(name, descriptor, access, transitive);
			}
		}
	}

	private static class ForwardingEnumExtensionVisitor implements EnumExtensionVisitor {
		private final EnumExtensionVisitor[] visitors;

		private ForwardingEnumExtensionVisitor(EnumExtensionVisitor[] visitors) {
			this.visitors = visitors.clone();
		}

		@Override
		public void visitParameterList(String owner, String name, String desc) {
			for (EnumExtensionVisitor visitor : visitors) {
				visitor.visitParameterList(owner, name, desc);
			}
		}

		@Override
		public void visitParameterConstants(Object[] constants) {
			for (EnumExtensionVisitor visitor : visitors) {
				visitor.visitParameterConstants(constants);
			}
		}

		@Override
		public void visitOverride(String methodName, String owner, String name, String desc) {
			for (EnumExtensionVisitor visitor : visitors) {
				visitor.visitOverride(methodName, owner, name, desc);
			}
		}

		@Override
		public void visitEnd() {
			for (EnumExtensionVisitor visitor : visitors) {
				visitor.visitEnd();
			}
		}
	}
}
