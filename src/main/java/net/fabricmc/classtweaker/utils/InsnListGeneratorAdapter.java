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

import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class InsnListGeneratorAdapter extends GeneratorAdapter {
	private final MethodNode methodNode = new MethodNode();

	public InsnListGeneratorAdapter(int api, int access, String name, String descriptor) {
		super(api, null, access, name, descriptor);
		mv = methodNode;
	}

	public InsnList getInsnList() {
		return methodNode.instructions;
	}
}
