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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.objectweb.asm.Type;

public class ConstantParser {
	public static List<Object> parseConstants(Type[] arguments, List<String> tokens) throws ConstantParseException {
		if (tokens.size() != arguments.length) {
			throw new ConstantParseException("Unexpected token size of (%s) for type (%s)", tokens, arguments.length);
		}

		final List<Object> constants = new ArrayList<>(arguments.length);

		for (int i = 0; i < arguments.length; i++) {
			constants.add(parseToken(tokens.get(i), arguments[i]));
		}

		assert constants.size() == arguments.length;
		return constants;
	}

	public static Object parseToken(String token, Type type) throws ConstantParseException {
		final int sort = AsmUtils.getBoxedSort(type);

		switch (sort) {
		case Type.CHAR: case Type.BYTE: case Type.SHORT: case Type.INT: case Type.LONG:
			long value;

			if (token.startsWith("'") && token.endsWith("'") && token.length() == 3) {
				// Char
				value = token.charAt(1);
			} else if (token.startsWith("0x")) {
				// Hex
				value = Long.parseLong(token.substring(2), 16);
			} else if (token.startsWith("0b")) {
				// Binary
				value = Long.parseLong(token.substring(2), 2);
			} else {
				value = Long.parseLong(token);
			}

			switch (sort) {
			case Type.CHAR:
				if (value > Character.MAX_VALUE || value < Character.MIN_VALUE) {
					throw new ConstantParseException("Character out of bounds (%s)", value);
				}

				return (char) value;
			case Type.BYTE:
				if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
					throw new ConstantParseException("Byte out of bounds (%s)", value);
				}

				return (byte) value;
			case Type.SHORT:
				if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
					throw new ConstantParseException("Short out of bounds (%s)", value);
				}

				return (short) value;
			case Type.INT:
				if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
					throw new ConstantParseException("Integer out of bounds (%s)", value);
				}

				return (int) value;
			case Type.LONG:
				return value;
			default:
				throw new IllegalStateException("How did we get here");
			}
		case Type.FLOAT:
			return Float.parseFloat(token);
		case Type.DOUBLE:
			return Double.parseDouble(token);
		case Type.BOOLEAN:
			if ("true".equals(token)) {
				return true;
			} else if ("false".equals(token)) {
				return false;
			}

			throw new ConstantParseException("Expected true or false, got (%s)", token);
		case Type.OBJECT:
			if ("null".equals(token)) {
				return null;
			}

			switch (type.getInternalName()) {
			case "java/lang/String":
				if (!token.startsWith("\"") || !token.endsWith("\"")) {
					throw new ConstantParseException("Invalid string token got (%s)", type.getInternalName());
				}

				return token.substring(1, token.length() -1);
			default:
				throw new ConstantParseException("Unsupported object of type (%s)", type.getInternalName());
			}

		default:
			throw new ConstantParseException("Unsupported constant type (%s)", type.getInternalName());
		}
	}

	public static boolean isConstant(String str) {
		return isLiteral(str)
				|| Character.isDigit(str.charAt(0)) // Number
				|| (str.length() > 2 && str.charAt(0) == '-' && Character.isDigit(str.charAt(1))) // Negative number
				|| str.charAt(0) == '"' // String
				|| str.charAt(0) == '\''; // Char
	}

	public static boolean isLiteral(String str) {
		switch (str) {
		case "null":
		case "true":
		case "false":
			return true;
		default:
			return false;
		}
	}

	public static class ConstantParseException extends Exception {
		public ConstantParseException(String message, Object... args) {
			super(String.format(Locale.ROOT, message, args));
		}
	}
}
