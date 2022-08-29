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

package net.fabricmc.classtweaker.utils;

import org.objectweb.asm.Opcodes;

public class AccessUtils {
	public static int makePublic(int i) {
		return (i & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
	}

	public static int makeProtected(int i) {
		if ((i & Opcodes.ACC_PUBLIC) != 0) {
			//Return i if public
			return i;
		}

		return (i & ~(Opcodes.ACC_PRIVATE)) | Opcodes.ACC_PROTECTED;
	}

	public static int makeFinalIfPrivate(int access, String name, int ownerAccess) {
		// Dont make constructors final
		if (name.equals("<init>")) {
			return access;
		}

		// Skip interface and static methods
		if ((ownerAccess & Opcodes.ACC_INTERFACE) != 0 || (access & Opcodes.ACC_STATIC) != 0) {
			return access;
		}

		if ((access & Opcodes.ACC_PRIVATE) != 0) {
			return access | Opcodes.ACC_FINAL;
		}

		return access;
	}

	public static int removeFinal(int i) {
		return i & ~Opcodes.ACC_FINAL;
	}
}
