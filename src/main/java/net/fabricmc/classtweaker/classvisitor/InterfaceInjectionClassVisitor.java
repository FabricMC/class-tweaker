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

package net.fabricmc.classtweaker.classvisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.InjectedInterface;

public class InterfaceInjectionClassVisitor extends ClassVisitor {
	private static final int INTERFACE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE;

	private final ClassTweaker classTweaker;
	private List<InjectedInterface> injectedInterfaces;
	private final Set<String> knownInnerClasses = new HashSet<>();

	public InterfaceInjectionClassVisitor(int api, ClassVisitor classVisitor, ClassTweaker classTweaker) {
		super(api, classVisitor);
		this.classTweaker = classTweaker;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		injectedInterfaces = classTweaker.getInjectedInterfaces(name);

		if (injectedInterfaces.isEmpty()) {
			super.visit(version, access, name, signature, superName, interfaces);
			return;
		}

		final Set<String> modifiedInterfaces = new LinkedHashSet<>();
		Collections.addAll(modifiedInterfaces, interfaces);

		StringBuilder newSignature = signature == null ? null : new StringBuilder(signature);

		if (newSignature == null && injectedInterfaces.stream().anyMatch(InjectedInterface::hasGenerics)) {
			// Classes that are not using generics don't need signatures, so their signatures are null
			// If the class is not using generics but that an injected interface targeting the class is using them, we are creating the class signature

			// See JVMS: https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-ClassSignature
			newSignature = new StringBuilder("L").append(superName).append(";");

			for (String baseInterface : interfaces) {
				newSignature.append("L").append(baseInterface).append(";");
			}
		}

		for (InjectedInterface injectedInterface : injectedInterfaces) {
			if (modifiedInterfaces.add(injectedInterface.getInterfaceName()) && newSignature != null) {
				newSignature.append(injectedInterface.getInterfaceSignature());
			}
		}

		if (newSignature != null) {
			signature = newSignature.toString();

			// If there are passed generics, are all of them present in the target class?
			SignatureReader reader = new SignatureReader(signature);

			GenericsChecker checker = new GenericsChecker(Opcodes.ASM9, name, injectedInterfaces);
			reader.accept(checker);
			checker.check();
		}

		super.visit(version, access, name, signature, superName, modifiedInterfaces.toArray(new String[0]));
	}

	@Override
	public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
		this.knownInnerClasses.add(name);
		super.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public void visitEnd() {
		// inject any necessary inner class entries
		// this may produce technically incorrect bytecode cuz we don't know the actual access flags for inner class entries,
		// but it's hopefully enough to quiet some IDE errors
		for (final InjectedInterface itf : injectedInterfaces) {
			final String ifaceName = itf.getInterfaceName();

			if (this.knownInnerClasses.contains(ifaceName)) {
				continue;
			}

			int simpleNameIdx = ifaceName.lastIndexOf('/');
			final String simpleName = simpleNameIdx == -1 ? ifaceName : ifaceName.substring(simpleNameIdx + 1);
			int lastIdx = -1;
			int dollarIdx = -1;

			// Iterate through inner class entries starting from outermost to innermost
			while ((dollarIdx = simpleName.indexOf('$', dollarIdx + 1)) != -1) {
				if (dollarIdx - lastIdx == 1) {
					continue;
				}

				// Emit the inner class entry from this to the last one
				if (lastIdx != -1) {
					final String outerName = ifaceName.substring(0, simpleNameIdx + 1 + lastIdx);
					final String innerName = simpleName.substring(lastIdx + 1, dollarIdx);
					super.visitInnerClass(outerName + '$' + innerName, outerName, innerName, INTERFACE_ACCESS);
				}

				lastIdx = dollarIdx;
			}

			// If we have a trailer to append
			if (lastIdx != -1 && lastIdx != simpleName.length()) {
				final String outerName = ifaceName.substring(0, simpleNameIdx + 1 + lastIdx);
				final String innerName = simpleName.substring(lastIdx + 1);
				super.visitInnerClass(outerName + '$' + innerName, outerName, innerName, INTERFACE_ACCESS);
			}
		}

		super.visitEnd();
	}

	private static class GenericsChecker extends SignatureVisitor {
		private final String className;
		private final List<String> typeParameters;
		private final List<InjectedInterface> injectedInterfaces;

		GenericsChecker(int asmVersion, String className, List<InjectedInterface> injectedInterfaces) {
			super(asmVersion);
			this.className = className;
			this.typeParameters = new ArrayList<>();
			this.injectedInterfaces = injectedInterfaces;
		}

		@Override
		public void visitFormalTypeParameter(String name) {
			this.typeParameters.add(name);
			super.visitFormalTypeParameter(name);
		}

		// Ensures that injected interfaces only use collected type parameters from the target class
		public void check() {
			for (InjectedInterface injectedInterface : this.injectedInterfaces) {
				if (injectedInterface.hasGenerics()) {
					SignatureReader reader = new SignatureReader(injectedInterface.getInterfaceSignature());
					GenericsConfirm confirm = new GenericsConfirm(
							Opcodes.ASM9,
							className,
							injectedInterface.getInterfaceName(),
							this.typeParameters
					);
					reader.accept(confirm);
				}
			}
		}

		public static class GenericsConfirm extends SignatureVisitor {
			private final String className;
			private final String interfaceName;
			private final List<String> acceptedTypeVariables;

			GenericsConfirm(int asmVersion, String className, String interfaceName, List<String> acceptedTypeVariables) {
				super(asmVersion);
				this.className = className;
				this.interfaceName = interfaceName;
				this.acceptedTypeVariables = acceptedTypeVariables;
			}

			@Override
			public void visitTypeVariable(String name) {
				if (!this.acceptedTypeVariables.contains(name)) {
					throw new IllegalStateException(
							"Interface "
									+ this.interfaceName
									+ " attempted to use a type variable named "
									+ name
									+ " which is not present in the "
									+ this.className
									+ " class"
					);
				}

				super.visitTypeVariable(name);
			}
		}
	}
}
