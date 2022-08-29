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

package net.fabricmc.classtweaker.api;

import java.util.Map;

import net.fabricmc.classtweaker.utils.EntryTriple;

public interface AccessWidener {
	Access getClassAccess();

	Access getMethodAccess(EntryTriple entryTriple);

	Access getFieldAccess(EntryTriple entryTriple);

	Map<EntryTriple, Access> getAllMethodAccesses();

	Map<EntryTriple, Access> getAllFieldAccesses();

	interface Access {
		boolean isAccessible();
		boolean isExtendable();
		boolean isMutable();

		default boolean isChanged() {
			return isAccessible() || isExtendable() || isMutable();
		}

		int apply(int access, String targetName, int ownerAccess);
	}
}
