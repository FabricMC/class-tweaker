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
import java.util.Set;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;

public interface ClassTweaker {
	// Access widener format versions
	int AW_V1 = 1;
	int AW_V2 = 2;
	// Class tweaker format versions.
	int CT_V1 = 3;

	/**
	 * The mapping namespace of the current class tweaker.
	 * @return the mappings namespace.
	 */
	String getNamespace();

	/**
	 * Contains a list of all classes that should be transformed.
	 * This may contain classes (usually parent classes) that do have a direct class tweak to be applied to them.
	 *
	 * <p>Names are period-separated binary names (i.e. a.b.C).
	 */
	Set<String> getTargets();

	/**
	 * Contains a list of all the classes that have class tweakers.
	 * Names are forward slash separated. (i.e a/b/C$I);
	 */
	Set<String> getClasses();

	AccessWidener getAccessWidener(String className);

	Map<String, AccessWidener> getAllAccessWideners();

	Map<String, EnumExtension> getEnumExtensions(String className);

	Map<String, Map<String, EnumExtension>> getAllEnumExtensions();

	Set<InjectedInterface> getInjectedInterfaces(String className);

	Map<String, Set<InjectedInterface>> getAllInjectedInterfaces();

	ClassVisitor createClassVisitor(int api, @Nullable ClassVisitor classVisitor, @Nullable BiConsumer<String, byte[]> generatedClassConsumer);
}
