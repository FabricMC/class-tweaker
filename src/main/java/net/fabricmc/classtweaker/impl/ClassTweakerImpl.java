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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;

import net.fabricmc.classtweaker.api.AccessWidener;
import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.InjectedInterface;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.classvisitor.AccessWidenerClassVisitor;
import net.fabricmc.classtweaker.classvisitor.InterfaceInjectionClassVisitor;

public final class ClassTweakerImpl implements ClassTweaker, ClassTweakerVisitor {
	String namespace;
	// Contains the actual transforms. Class names are as class-file internal binary names (forward slash is used
	// instead of period as the package separator).
	final Map<String, AccessWidenerImpl> accessWideners = new HashMap<>();
	final Map<String, List<InjectedInterfaceImpl>> injectedInterfaces = new HashMap<>();
	// Contains the class-names that are affected by loaded tweakers.
	// Names are period-separated binary names (i.e. a.b.C).
	final Set<String> targetClasses = new LinkedHashSet<>();
	final Set<String> classes = new LinkedHashSet<>();

	@Override
	public void visitHeader(String namespace) {
		if (this.namespace != null && !this.namespace.equals(namespace)) {
			throw new RuntimeException(String.format("Namespace mismatch, expected %s got %s", this.namespace, namespace));
		}

		this.namespace = namespace;
	}

	@Override
	public AccessWidenerVisitor visitAccessWidener(String owner) {
		AccessWidenerImpl accessWidener = accessWideners.get(owner);

		if (accessWidener == null) {
			accessWidener = new AccessWidenerImpl(owner);
			accessWideners.put(owner, accessWidener);
			addTargets(owner);
		}

		return accessWidener;
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		final List<InjectedInterfaceImpl> injectedInterfaces = this.injectedInterfaces.computeIfAbsent(owner, s -> new ArrayList<>());
		final InjectedInterfaceImpl injectedInterface = new InjectedInterfaceImpl(iface);

		injectedInterfaces.add(injectedInterface);
		addTargets(owner);
	}

	private void addTargets(String clazz) {
		classes.add(clazz);
		clazz = clazz.replace('/', '.');
		targetClasses.add(clazz);

		//Also transform all parent classes
		while (clazz.contains("$")) {
			clazz = clazz.substring(0, clazz.lastIndexOf("$"));
			targetClasses.add(clazz);
		}
	}

	@Override
	public ClassVisitor createClassVisitor(int api, @Nullable ClassVisitor classVisitor, @Nullable BiConsumer<String, byte[]> generatedClassConsumer) {
		if (!accessWideners.isEmpty()) {
			classVisitor = new AccessWidenerClassVisitor(api, classVisitor, this);
		}

		if (!injectedInterfaces.isEmpty()) {
			classVisitor = new InterfaceInjectionClassVisitor(api, classVisitor, this);
		}

		return classVisitor;
	}

	@Override
	public Set<String> getTargets() {
		return Collections.unmodifiableSet(targetClasses);
	}

	@Override
	public Set<String> getClasses() {
		return Collections.unmodifiableSet(classes);
	}

	@Override
	public AccessWidener getAccessWidener(String className) {
		AccessWidenerImpl accessWidener = accessWideners.get(className);

		if (accessWidener == null) {
			return AccessWidenerImpl.DEFAULT;
		}

		return accessWidener;
	}

	@Override
	public Map<String, AccessWidener> getAllAccessWideners() {
		//noinspection unchecked
		return Collections.unmodifiableMap((Map) accessWideners);
	}

	@Override
	public List<InjectedInterface> getInjectedInterfaces(String className) {
		return Collections.unmodifiableList(injectedInterfaces.getOrDefault(className, Collections.emptyList()));
	}

	@Override
	public Map<String, List<InjectedInterface>> getAllInjectedInterfaces() {
		//noinspection unchecked
		return Collections.unmodifiableMap((Map) injectedInterfaces);
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespace, accessWideners, targetClasses, classes);
	}
}
