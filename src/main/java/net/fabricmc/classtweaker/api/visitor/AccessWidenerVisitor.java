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

package net.fabricmc.classtweaker.api.visitor;

public interface AccessWidenerVisitor {
	/**
	 * Visits a widened class.
	 *
	 * @param access the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#EXTENDABLE})
	 * @param transitive whether this widener should be applied across mod boundaries
	 */
	default void visitClass(AccessType access, boolean transitive) {
	}

	/**
	 * Visits a widened method.
	 *
	 * @param name the name of the method
	 * @param descriptor the method descriptor
	 * @param access the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#EXTENDABLE})
	 * @param transitive whether this widener should be applied across mod boundaries
	 */
	default void visitMethod(String name, String descriptor, AccessType access, boolean transitive) {
	}

	/**
	 * Visits a widened field.
	 *
	 * @param name the name of the field
	 * @param descriptor the type of the field as a type descriptor
	 * @param access the access type ({@link AccessType#ACCESSIBLE} or {@link AccessType#MUTABLE})
	 * @param transitive whether this widener should be applied across mod boundaries
	 */
	default void visitField(String name, String descriptor, AccessType access, boolean transitive) {
	}

	enum AccessType {
		ACCESSIBLE("accessible"),
		EXTENDABLE("extendable"),
		MUTABLE("mutable");

		private final String id;

		AccessType(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return id;
		}
	}
}
