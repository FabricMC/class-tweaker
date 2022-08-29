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

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import net.fabricmc.classtweaker.utils.EntryTriple;

public interface EnumExtension {
	/**
	 * The enum field name to be added to the target enum.
	 */
	String getName();

	/**
	 * A unique id to help reference the enum extension.
	 * Will be the modid when used with fabric-loader.
	 */
	String getId();

	/**
	 * The existing enum constructor to be used for the enum addition.
	 */
	Type getTargetConstructor();

	/**
	 * A list of {@link MethodOverride}.
	 * When not empty an inner class will be generated.
	 */
	List<MethodOverride> getMethodOverrides();

	/**
	 * Returns a nullable {@link Parameters} containing the data required to constructor the enum entry.
	 */
	@Nullable EnumExtension.Parameters getParameters();

	interface MethodOverride {
		/**
		 * The method name to override the descriptor comes from the static method.
		 */
		String getTargetMethodName();

		/**
		 * The static method to call from the override, the descriptor must match the target method.
		 */
		EntryTriple getStaticMethod();
	}

	interface Parameters {
	}

	interface ListParameters extends Parameters {
		/**
		 * A {@link EntryTriple} pointing to a public static {@link List} of enum values.
		 * This allows the passing in of complex objects to the constructor of the enum.
		 */
		EntryTriple getParamList();
	}

	interface ConstantParameters extends Parameters {
		/**
		 * An array of boxed primitive types.
		 *
		 * <p>Will only ever contain:
		 * - null
		 * - {@link String}
		 * - {@link Character}
		 * - {@link Byte}
		 * - {@link Short}
		 * - {@link Integer}
		 * - {@link Long}
		 * - {@link Float}
		 * - {@link Double}
		 * - {@link Boolean}
		 */
		Object[] getConstants();
	}
}
