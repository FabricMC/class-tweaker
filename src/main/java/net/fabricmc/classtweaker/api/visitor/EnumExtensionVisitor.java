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

public interface EnumExtensionVisitor {
	/**
	 * Visit a static list field containing the constructor arguments.
	 *
	 * @param owner Owner class
	 * @param name Field name
	 */
	default void visitParameterList(String owner, String name, String desc) {
	}

	default void visitParameterConstants(Object[] constants) {
	}

	default void visitOverride(String methodName, String owner, String name, String desc) {
	}

	default void visitEnd() {
	}
}
