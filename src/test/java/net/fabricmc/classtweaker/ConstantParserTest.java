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

package net.fabricmc.classtweaker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import net.fabricmc.classtweaker.utils.ConstantParser;

public class ConstantParserTest {
	@Test
	void parseBoolTrue() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(Z)V", "true");
		assertThat(constants).containsExactly(true);
	}

	@Test
	void parseBoolFalse() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(Z)V", "false");
		assertThat(constants).containsExactly(false);
	}

	@Test
	void parseByte() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(B)V", "123");
		assertThat(constants).containsExactly((byte) 123);
	}

	@Test
	void parseShort() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(S)V", "1234");
		assertThat(constants).containsExactly((short) 1234);
	}

	@Test
	void parseChar() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(C)V", "'H'");
		assertThat(constants).containsExactly('H');
	}

	@Test
	void parseInt() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(I)V", "123");
		assertThat(constants).containsExactly(123);
	}

	@Test
	void parseLong() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(J)V", "123456789");
		assertThat(constants).containsExactly(123456789L);
	}

	@Test
	void parseFloat() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(F)V", "1.55");
		assertThat(constants).containsExactly(1.55F);
	}

	@Test
	void parseDouble() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(D)V", "3.22");
		assertThat(constants).containsExactly(3.22D);
	}

	@Test
	void parseHexInt() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(I)V", "0xABC");
		assertThat(constants).containsExactly(0xABC);
	}

	@Test
	void parseBinInt() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(I)V", "0b101");
		assertThat(constants).containsExactly(0b101);
	}

	@Test
	void parseNegativeInt() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(I)V", "-5002");
		assertThat(constants).containsExactly(-5002);
	}

	@Test
	void parseString() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(Ljava/lang/String;)V", "\"Hello World\"");
		assertThat(constants).containsExactly("Hello World");
	}

	@Test
	void parseMany() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(Ljava/lang/String;ZBSCIJ)V",
				"\"Hello World\"",
				"true",
				"55",
				"555",
				"'H'",
				"1024",
				"102400000"
		);
		assertThat(constants).containsExactly(
				"Hello World",
				true,
				(byte) 55,
				(short) 555,
				'H',
				1024,
				102400000L
		);
	}

	@Test
	void parseBoxed() throws ConstantParser.ConstantParseException {
		List<Object> constants = parse("(Ljava/lang/Boolean;Ljava/lang/Character;Ljava/lang/Byte;Ljava/lang/Short;Ljava/lang/Integer;Ljava/lang/Float;Ljava/lang/Long;Ljava/lang/Double;)V",
				"true",
				"'H'",
				"55",
				"555",
				"1024",
				"1.25",
				"102400000",
				"2.25"
		);
		assertThat(constants).containsExactly(
				true,
				'H',
				(byte) 55,
				(short) 555,
				1024,
				1.25F,
				102400000L,
				2.25D
		);
	}

	private List<Object> parse(String desc, String... tokens) throws ConstantParser.ConstantParseException {
		return parse(Type.getType(desc), tokens);
	}

	private List<Object> parse(Type type, String... tokens) throws ConstantParser.ConstantParseException {
		return ConstantParser.parseConstants(type.getArgumentTypes(), Arrays.stream(tokens).collect(Collectors.toList()));
	}
}
