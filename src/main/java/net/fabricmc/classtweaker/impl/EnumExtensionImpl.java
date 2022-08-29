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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import net.fabricmc.classtweaker.api.EnumExtension;
import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;
import net.fabricmc.classtweaker.utils.EntryTriple;

public class EnumExtensionImpl implements EnumExtension, EnumExtensionVisitor {
	private final String name;
	private final Type targetConstructor;
	private final String id;

	private Parameters params = null;
	private final List<MethodOverride> methodOverrides = new ArrayList<>();

	public EnumExtensionImpl(String name, Type targetConstructor, String id) {
		this.name = name;
		this.targetConstructor = targetConstructor;
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Type getTargetConstructor() {
		return targetConstructor;
	}

	@Nullable
	public EnumExtension.Parameters getParameters() {
		return params;
	}

	@Override
	public void visitParameterList(String owner, String name, String desc) {
		setParams(new ListParametersImpl(new EntryTriple(owner, name, desc)));
	}

	@Override
	public void visitParameterConstants(Object[] constants) {
		setParams(new ConstantParametersImpl(constants));
	}

	@Override
	public void visitOverride(String methodName, String owner, String name, String desc) {
		methodOverrides.add(new MethodOverrideImpl(methodName, new EntryTriple(owner, name, desc)));
	}

	@Override
	public List<MethodOverride> getMethodOverrides() {
		return Collections.unmodifiableList(methodOverrides);
	}

	private void setParams(Parameters params) {
		if (this.params != null) {
			throw new RuntimeException("Target enum already has constructor parameters");
		}

		this.params = params;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, targetConstructor, params, methodOverrides);
	}

	public class ListParametersImpl implements ListParameters {
		private final EntryTriple paramList;

		private ListParametersImpl(EntryTriple paramList) {
			this.paramList = paramList;
		}

		public EntryTriple getParamList() {
			return paramList;
		}

		@Override
		public int hashCode() {
			return Objects.hash(paramList);
		}
	}

	public class ConstantParametersImpl implements ConstantParameters {
		// TODO validate this against the target desc
		private final Object[] constants;

		private ConstantParametersImpl(Object[] constants) {
			this.constants = constants;
		}

		public Object[] getConstants() {
			return constants;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(constants);
		}
	}

	public static class MethodOverrideImpl implements MethodOverride {
		private final String targetMethodName;
		private final EntryTriple staticMethod;

		public MethodOverrideImpl(String targetMethodName, EntryTriple staticMethod) {
			this.targetMethodName = targetMethodName;
			this.staticMethod = staticMethod;
		}

		public String getTargetMethodName() {
			return targetMethodName;
		}

		public EntryTriple getStaticMethod() {
			return staticMethod;
		}

		@Override
		public int hashCode() {
			return Objects.hash(targetMethodName, staticMethod);
		}
	}
}
