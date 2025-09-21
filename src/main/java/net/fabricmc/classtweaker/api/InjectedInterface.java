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

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface InjectedInterface {
	/**
	 * The class name of the interface to inject onto the target class.
	 */
	String getInterfaceName();

	/**
	 * The signature of the interface to inject, including any generics. If no generics are present, this will be
	 * {@code "L" + interfaceName + ";"}. See the
	 * <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-ClassTypeSignature">JVMS</a>.
	 */
	String getInterfaceSignature();

	/**
	 * Whether the interface to inject has any generics.
	 */
	boolean hasGenerics();
}
