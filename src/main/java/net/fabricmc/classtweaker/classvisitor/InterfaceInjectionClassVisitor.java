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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;

import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.InjectedInterface;

public class InterfaceInjectionClassVisitor extends ClassVisitor {
	private final ClassTweaker classTweaker;

	public InterfaceInjectionClassVisitor(int api, ClassVisitor classVisitor, ClassTweaker classTweaker) {
		super(api, classVisitor);
		this.classTweaker = classTweaker;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		final Set<InjectedInterface> injectedInterfaces = classTweaker.getInjectedInterfaces(name);

		if (injectedInterfaces.isEmpty()) {
			super.visit(version, access, name, signature, superName, interfaces);
			return;
		}

		final Set<String> modifiedInterfaces = new LinkedHashSet<>(interfaces.length + injectedInterfaces.size());
		Collections.addAll(modifiedInterfaces, interfaces);

		for (InjectedInterface injectedInterface : injectedInterfaces) {
			modifiedInterfaces.add(injectedInterface.getInterfaceName());
		}

		// See JVMS: https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-ClassSignature
		if (signature != null) {
			final StringBuilder resultingSignature = new StringBuilder(signature);

			for (InjectedInterface injectedInterface : injectedInterfaces) {
				final String superinterfaceSignature = "L" + injectedInterface.getInterfaceName() + ";";

				if (resultingSignature.indexOf(superinterfaceSignature) == -1) {
					resultingSignature.append(superinterfaceSignature);
				}
			}

			signature = resultingSignature.toString();
		}

		super.visit(version, access, name, signature, superName, modifiedInterfaces.toArray(new String[0]));
	}
}
