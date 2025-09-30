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

package net.fabricmc.classtweaker.impl;

import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import net.fabricmc.classtweaker.api.InjectedInterface;

public class InjectedInterfaceImpl implements InjectedInterface {
	private final String injectedInterface;

	public InjectedInterfaceImpl(String injectedInterface) {
		this.injectedInterface = injectedInterface;
	}

	@Override
	public String getInterfaceName() {
		if (!hasGenerics()) {
			return injectedInterface;
		}

		RawTypeFromSignatureVisitor rawTypeFromSignatureVisitor = new RawTypeFromSignatureVisitor();
		new SignatureReader("L" + injectedInterface + ";").accept(rawTypeFromSignatureVisitor);
		return rawTypeFromSignatureVisitor.rawType.toString();
	}

	@Override
	public String getInterfaceSignature() {
		return "L" + injectedInterface + ";";
	}

	@Override
	public boolean hasGenerics() {
		return injectedInterface.contains("<");
	}

	@Override
	public int hashCode() {
		return Objects.hash(injectedInterface);
	}

	private static final class RawTypeFromSignatureVisitor extends SignatureVisitor {
		private final StringBuilder rawType = new StringBuilder();

		RawTypeFromSignatureVisitor() {
			super(Opcodes.ASM9);
		}

		@Override
		public void visitClassType(String name) {
			rawType.append(name);
		}

		@Override
		public void visitInnerClassType(String name) {
			rawType.append('$').append(name);
		}

		@Override
		public SignatureVisitor visitTypeArgument(char wildcard) {
			// don't include anything inside the type arguments in the raw type, return a dummy visitor.
			return new SignatureVisitor(Opcodes.ASM9) {
			};
		}
	}
}
