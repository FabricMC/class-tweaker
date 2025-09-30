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
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		for (ClassTweakerVisitor visitor : visitors) {
			visitor.visitInjectedInterface(owner, iface, transitive);
		}
	}

	@Override
	public void visitLineNumber(int lineNumber) {
		for (ClassTweakerVisitor visitor : visitors) {
			visitor.visitLineNumber(lineNumber);
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
}
