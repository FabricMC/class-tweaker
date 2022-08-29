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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.classtweaker.impl.ClassTweakerImpl;

public abstract class ClassVisitorTest {
	public ClassTweakerImpl classTweaker = new ClassTweakerImpl();

	/**
	 * Applies a given access transformer but also ensures that the given class is the only class that is affected.
	 */
	public Class<?> applyTransformer(String className) throws Exception {
		Map<String, Class<?>> classes = applyTransformer();
		assertThat(classes).containsOnlyKeys(className);
		return classes.get(className);
	}

	/**
	 * Loads all classes from the "test" package with the given access widener applied. Only returns the classes
	 * that have actually been affected by the widener.
	 */
	public Map<String, Class<?>> applyTransformer() throws Exception {
		TransformingClassLoader classLoader = new TransformingClassLoader(getClass().getClassLoader());

		// This assumes that tests are being run from an exploded classpath and not a jar file, which is the case
		// for both IDE and Gradle runs.
		Path classFile = Paths.get(getClass().getResource("/test/PackagePrivateClass.class").toURI());
		Path classFolder = classFile.getParent();
		return Files.walk(classFolder)
				// Map /test/X.class -> X.class
				.map(p -> p.getFileName().toString())
				.filter(p -> p.endsWith(".class"))
				.map(p -> "test." + p.substring(0, p.length() - ".class".length()))
				.filter(classTweaker.getTargets()::contains)
				.collect(Collectors.toMap(
						p -> p,
						p -> {
							try {
								return Class.forName(p, false, classLoader);
							} catch (ClassNotFoundException e) {
								return fail(e);
							}
						}
				));
	}

	public ClassNode readClass(String className) {
		try (InputStream classData = getClass().getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
			ClassReader classReader = new ClassReader(Objects.requireNonNull(classData));
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode, 0);

			return classNode;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private class TransformingClassLoader extends ClassLoader {
		TransformingClassLoader(ClassLoader parent) {
			super(parent);
		}

		private final Map<String, byte[]> generatedClasses = new HashMap<>();

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			byte[] generatedBytes = generatedClasses.remove(name.replace(".", "/"));

			if (generatedBytes != null) {
				writeClass(name, generatedBytes);
				return defineClass(name, generatedBytes, 0, generatedBytes.length);
			}

			if (name.startsWith("test.")) {
				InputStream classData = getParent().getResourceAsStream(name.replace('.', '/') + ".class");

				if (classData != null) {
					if (classTweaker.getTargets().contains(name)) {
						try {
							ClassReader classReader = new ClassReader(classData);
							ClassWriter classWriter = new ClassWriter(0);
							ClassVisitor visitor = classWriter;
							visitor = classTweaker.createClassVisitor(Opcodes.ASM9, visitor, generatedClasses::put);
							classReader.accept(visitor, 0);
							byte[] bytes = classWriter.toByteArray();

							writeClass(name, bytes);
							return defineClass(name, bytes, 0, bytes.length);
						} catch (IOException e) {
							throw new ClassNotFoundException();
						}
					} else {
						ByteArrayOutputStream buffer = new ByteArrayOutputStream();

						try {
							int i;
							byte[] data = new byte[16384];

							while ((i = classData.read(data, 0, data.length)) != -1) {
								buffer.write(data, 0, i);
							}
						} catch (IOException e) {
							throw new ClassNotFoundException();
						}

						byte[] bytes = buffer.toByteArray();

						writeClass(name, bytes);
						return defineClass(name, bytes, 0, bytes.length);
					}
				}
			}

			return super.loadClass(name, resolve);
		}

		private void writeClass(String name, byte[] bytes) {
			try {
				Path out = Paths.get("test", "classes", name.substring(name.lastIndexOf(".") + 1) + ".class");
				Files.createDirectories(out.getParent());
				Files.write(out, bytes);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
