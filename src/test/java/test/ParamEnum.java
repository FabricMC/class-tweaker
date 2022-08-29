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

package test;

public enum ParamEnum {
	A("a", 100),
	B("b", 200),
	C("c", 300),
	D("d", 400),
	E("e", 500),
	F("f", 600),
	G("g", 700);

	final String letter;
	final int number;

	ParamEnum(String letter, int number) {
		this.letter = letter;
		this.number = number;
	}
}
