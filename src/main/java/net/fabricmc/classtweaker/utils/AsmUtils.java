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

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class AsmUtils {
	private AsmUtils() {
	}

	public static int getBoxedSort(Type type) {
		switch (type.getInternalName()) {
		case "java/lang/Boolean":
			return Type.BOOLEAN;
		case "java/lang/Character":
			return Type.CHAR;
		case "java/lang/Byte":
			return Type.BYTE;
		case "java/lang/Short":
			return Type.SHORT;
		case "java/lang/Integer":
			return Type.INT;
		case "java/lang/Float":
			return Type.FLOAT;
		case "java/lang/Long":
			return Type.LONG;
		case "java/lang/Double":
			return Type.DOUBLE;
		default:
			return type.getSort();
		}
	}

	@Nullable
	public static EntryTriple getUnboxMethod(Type type) {
		switch (type.getSort()) {
		case Type.BOOLEAN:
			return new EntryTriple("java/lang/Boolean", "booleanValue", "()Z");
		case Type.CHAR:
			return new EntryTriple("java/lang/Character", "charValue", "()C");
		case Type.BYTE:
			return new EntryTriple("java/lang/Byte", "byteValue", "()B");
		case Type.SHORT:
			return new EntryTriple("java/lang/Short", "shortValue", "()S");
		case Type.INT:
			return new EntryTriple("java/lang/Integer", "intValue", "()I");
		case Type.FLOAT:
			return new EntryTriple("java/lang/Float", "floatValue", "()F");
		case Type.LONG:
			return new EntryTriple("java/lang/Long", "longValue", "()J");
		case Type.DOUBLE:
			return new EntryTriple("java/lang/Double", "doubleValue", "()D");
		default:
			return null;
		}
	}

	public static boolean isIntConst(int opcode) {
		return opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5;
	}
}
