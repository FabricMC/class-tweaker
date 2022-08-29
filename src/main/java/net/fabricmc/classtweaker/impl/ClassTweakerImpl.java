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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import net.fabricmc.classtweaker.api.AccessWidener;
import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.EnumExtension;
import net.fabricmc.classtweaker.api.InjectedInterface;
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.api.visitor.EnumExtensionVisitor;
import net.fabricmc.classtweaker.classvisitor.AccessWidenerClassVisitor;
import net.fabricmc.classtweaker.classvisitor.EnumExtensionClassVisitor;
import net.fabricmc.classtweaker.classvisitor.InterfaceInjectionClassVisitor;

public final class ClassTweakerImpl implements ClassTweaker, ClassTweakerVisitor {
	String namespace;
	// Contains the actual transforms. Class names are as class-file internal binary names (forward slash is used
	// instead of period as the package separator).
	final Map<String, AccessWidenerImpl> accessWideners = new HashMap<>();
	final Map<String, Map<String, EnumExtensionImpl>> enumExtensions = new HashMap<>();
	final Map<String, Set<InjectedInterfaceImpl>> injectedInterfaces = new HashMap<>();
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
	public EnumExtensionVisitor visitEnum(String owner, String name, String constructorDesc, String id, boolean transitive) {
		final Map<String, EnumExtensionImpl> enumExtensions = this.enumExtensions.computeIfAbsent(owner, s -> new TreeMap<>());

		if (enumExtensions.containsKey(name)) {
			throw new RuntimeException(String.format("Duplicate enum extension value name (%s) in enum (%s)", name, owner));
		}

		final EnumExtensionImpl enumExtension = new EnumExtensionImpl(name, Type.getType(constructorDesc), id);
		enumExtensions.put(name, enumExtension);
		addTargets(owner);

		return enumExtension;
	}

	@Override
	public void visitInjectedInterface(String owner, String iface, boolean transitive) {
		final Set<InjectedInterfaceImpl> injectedInterfaces = this.injectedInterfaces.computeIfAbsent(owner, s -> new HashSet<>());
		final InjectedInterfaceImpl injectedInterface = new InjectedInterfaceImpl(iface);

		if (injectedInterfaces.contains(injectedInterface)) {
			throw new RuntimeException(String.format("Duplicate interface injection (%s) for class (%s)", iface, owner));
		}

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

		if (!enumExtensions.isEmpty()) {
			classVisitor = new EnumExtensionClassVisitor(api, classVisitor, this, generatedClassConsumer);
		}

		if (!injectedInterfaces.isEmpty()) {
			classVisitor = new InterfaceInjectionClassVisitor(api, classVisitor, this);
		}

		return classVisitor;
	}

	@Override
	public Map<String, EnumExtension> getEnumExtensions(String className) {
		//noinspection unchecked
		return Collections.unmodifiableMap((Map) enumExtensions.getOrDefault(className.replace(".", "/"), Collections.emptyMap()));
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
	public Map<String, Map<String, EnumExtension>> getAllEnumExtensions() {
		//noinspection unchecked
		return Collections.unmodifiableMap((Map) enumExtensions);
	}

	@Override
	public Set<InjectedInterface> getInjectedInterfaces(String className) {
		return Collections.unmodifiableSet(injectedInterfaces.getOrDefault(className, Collections.emptySet()));
	}

	@Override
	public Map<String, Set<InjectedInterface>> getAllInjectedInterfaces() {
		//noinspection unchecked
		return Collections.unmodifiableMap((Map) injectedInterfaces);
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespace, accessWideners, enumExtensions, targetClasses, classes);
	}
}
