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

package net.fabricmc.classtweaker.impl;

import static net.fabricmc.classtweaker.utils.AccessUtils.makeFinalIfPrivate;
import static net.fabricmc.classtweaker.utils.AccessUtils.makeProtected;
import static net.fabricmc.classtweaker.utils.AccessUtils.makePublic;
import static net.fabricmc.classtweaker.utils.AccessUtils.removeFinal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.Opcodes;

import net.fabricmc.classtweaker.utils.EntryTriple;
import net.fabricmc.classtweaker.api.AccessWidener;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;

public final class AccessWidenerImpl implements AccessWidener, AccessWidenerVisitor {
	private final String owner;

	MutableAccess classAccess = ClassAccess.DEFAULT;
	final Map<EntryTriple, MutableAccess> methodAccess = new HashMap<>();
	final Map<EntryTriple, MutableAccess> fieldAccess = new HashMap<>();

	public AccessWidenerImpl(String owner) {
		this.owner = owner;
	}

	@Override
	public MutableAccess getClassAccess() {
		return classAccess;
	}

	@Override
	public Access getMethodAccess(EntryTriple entryTriple) {
		final Access access = methodAccess.get(entryTriple);

		if (access == null) {
			return MutableAccess.DEFAULT;
		}

		return access;
	}

	@Override
	public Access getFieldAccess(EntryTriple entryTriple) {
		final Access access = fieldAccess.get(entryTriple);

		if (access == null) {
			return MutableAccess.DEFAULT;
		}

		return access;
	}

	@Override
	public Map<EntryTriple, Access> getAllMethodAccesses() {
		return (Map) methodAccess;
	}

	@Override
	public Map<EntryTriple, Access> getAllFieldAccesses() {
		return (Map) fieldAccess;
	}

	@Override
	public void visitClass(AccessWidenerVisitor.AccessType access, boolean transitive) {
		classAccess = applyAccess(access, classAccess, null);
	}

	@Override
	public void visitMethod(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
		addOrMerge(methodAccess, new EntryTriple(owner, name, descriptor), access, MethodAccess.DEFAULT);
	}

	@Override
	public void visitField(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
		addOrMerge(fieldAccess, new EntryTriple(owner, name, descriptor), access, FieldAccess.DEFAULT);
	}

	MutableAccess applyAccess(AccessWidenerVisitor.AccessType input, MutableAccess access, EntryTriple entryTriple) {
		switch (input) {
		case ACCESSIBLE:
			makeClassAccessible(entryTriple);
			return access.makeAccessible();
		case EXTENDABLE:
			makeClassExtendable(entryTriple);
			return access.makeExtendable();
		case MUTABLE:
			return access.makeMutable();
		default:
			throw new UnsupportedOperationException("Unknown access type:" + input);
		}
	}

	private void makeClassAccessible(EntryTriple entryTriple) {
		if (entryTriple == null) return;
		classAccess = applyAccess(AccessWidenerVisitor.AccessType.ACCESSIBLE, classAccess, null);
	}

	private void makeClassExtendable(EntryTriple entryTriple) {
		if (entryTriple == null) return;
		classAccess = applyAccess(AccessWidenerVisitor.AccessType.EXTENDABLE, classAccess, null);
	}

	void addOrMerge(Map<EntryTriple, MutableAccess> map, EntryTriple entry, AccessWidenerVisitor.AccessType access, MutableAccess defaultAccess) {
		if (entry == null || access == null) {
			throw new RuntimeException("Input entry or access is null");
		}

		map.put(entry, applyAccess(access, map.getOrDefault(entry, defaultAccess), entry));
	}

	interface MutableAccess extends Access {
		MutableAccess makeAccessible();

		MutableAccess makeExtendable();

		MutableAccess makeMutable();

		Access DEFAULT = new Access() {
			@Override
			public boolean isAccessible() {
				return false;
			}

			@Override
			public boolean isExtendable() {
				return false;
			}

			@Override
			public boolean isMutable() {
				return false;
			}

			@Override
			public int apply(int access, String targetName, int ownerAccess) {
				return access;
			}
		};
	}

	@FunctionalInterface
	public interface AccessOperator {
		int apply(int access, String targetName, int ownerAccess);
	}

	@VisibleForTesting
	public enum ClassAccess implements MutableAccess {
		DEFAULT((access, name, ownerAccess) -> access),
		ACCESSIBLE((access, name, ownerAccess) -> makePublic(access)),
		EXTENDABLE((access, name, ownerAccess) -> makePublic(removeFinal(access))),
		ACCESSIBLE_EXTENDABLE((access, name, ownerAccess) -> makePublic(removeFinal(access)));

		private final AccessOperator operator;

		ClassAccess(AccessOperator operator) {
			this.operator = operator;
		}

		@Override
		public MutableAccess makeAccessible() {
			if (this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return ACCESSIBLE;
		}

		@Override
		public MutableAccess makeExtendable() {
			if (isAccessible()) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return EXTENDABLE;
		}

		@Override
		public MutableAccess makeMutable() {
			throw new UnsupportedOperationException("Classes cannot be made mutable");
		}

		@Override
		public boolean isAccessible() {
			return this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE;
		}

		@Override
		public boolean isExtendable() {
			return this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return operator.apply(access, targetName, ownerAccess);
		}
	}

	@VisibleForTesting
	public enum MethodAccess implements MutableAccess {
		DEFAULT((access, name, ownerAccess) -> access),
		ACCESSIBLE((access, name, ownerAccess) -> makePublic(makeFinalIfPrivate(access, name, ownerAccess))),
		EXTENDABLE((access, name, ownerAccess) -> makeProtected(removeFinal(access))),
		ACCESSIBLE_EXTENDABLE((access, name, owner) -> makePublic(removeFinal(access)));

		private final AccessOperator operator;

		MethodAccess(AccessOperator operator) {
			this.operator = operator;
		}

		@Override
		public MutableAccess makeAccessible() {
			if (isExtendable()) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return ACCESSIBLE;
		}

		@Override
		public MutableAccess makeExtendable() {
			if (isAccessible()) {
				return ACCESSIBLE_EXTENDABLE;
			}

			return EXTENDABLE;
		}

		@Override
		public MutableAccess makeMutable() {
			throw new UnsupportedOperationException("Methods cannot be made mutable");
		}

		@Override
		public boolean isAccessible() {
			return this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE;
		}

		@Override
		public boolean isExtendable() {
			return this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return operator.apply(access, targetName, ownerAccess);
		}
	}

	@VisibleForTesting
	public enum FieldAccess implements MutableAccess {
		DEFAULT((access, name, ownerAccess) -> access),
		ACCESSIBLE((access, name, ownerAccess) -> makePublic(access)),
		MUTABLE((access, name, ownerAccess) -> {
			if ((ownerAccess & Opcodes.ACC_INTERFACE) != 0 && (access & Opcodes.ACC_STATIC) != 0) {
				// Don't make static interface fields mutable.
				return access;
			}

			return removeFinal(access);
		}),
		ACCESSIBLE_MUTABLE((access, name, ownerAccess) -> {
			if ((ownerAccess & Opcodes.ACC_INTERFACE) != 0 && (access & Opcodes.ACC_STATIC) != 0) {
				// Don't make static interface fields mutable.
				return makePublic(access);
			}

			return makePublic(removeFinal(access));
		});

		private final AccessOperator operator;

		FieldAccess(AccessOperator operator) {
			this.operator = operator;
		}

		@Override
		public MutableAccess makeAccessible() {
			if (isMutable()) {
				return ACCESSIBLE_MUTABLE;
			}

			return ACCESSIBLE;
		}

		@Override
		public MutableAccess makeExtendable() {
			throw new UnsupportedOperationException("Fields cannot be made extendable");
		}

		@Override
		public MutableAccess makeMutable() {
			if (isAccessible()) {
				return ACCESSIBLE_MUTABLE;
			}

			return MUTABLE;
		}

		@Override
		public boolean isAccessible() {
			return this == ACCESSIBLE || this == ACCESSIBLE_MUTABLE;
		}

		@Override
		public boolean isExtendable() {
			return false;
		}

		@Override
		public boolean isMutable() {
			return this == MUTABLE || this == ACCESSIBLE_MUTABLE;
		}

		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return operator.apply(access, targetName, ownerAccess);
		}
	}

	static final AccessWidener DEFAULT = new AccessWidener() {
		@Override
		public Access getClassAccess() {
			return MutableAccess.DEFAULT;
		}

		@Override
		public Access getMethodAccess(EntryTriple EntryTriple) {
			return MutableAccess.DEFAULT;
		}

		@Override
		public Access getFieldAccess(EntryTriple entryTriple) {
			return MutableAccess.DEFAULT;
		}

		@Override
		public Map<EntryTriple, Access> getAllMethodAccesses() {
			return Collections.emptyMap();
		}

		@Override
		public Map<EntryTriple, Access> getAllFieldAccesses() {
			return Collections.emptyMap();
		}
	};
}
