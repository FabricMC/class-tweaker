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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.EnumExtension;
import net.fabricmc.classtweaker.utils.AccessUtils;
import net.fabricmc.classtweaker.utils.AsmUtils;
import net.fabricmc.classtweaker.utils.EntryTriple;
import net.fabricmc.classtweaker.utils.InsnListGeneratorAdapter;

public final class EnumExtensionClassVisitor extends ClassVisitor {
	private static final String ANNOTATION_DESC = "Lnet/fabricmc/accesswidener/Extended;";
	private final ClassTweaker classTweaker;
	@Nullable
	private final BiConsumer<String, byte[]> generatedClassConsumer;

	private Map<String, EnumExtension> enumExtensions = Collections.emptyMap();
	private boolean generateOverrides = false;
	private String className;
	private int version;
	private boolean wroteEnumFields = false;
	private final Map<String, String> innerClassNames = new HashMap<>();
	private final Set<EntryTriple> methods = new HashSet<>();

	public EnumExtensionClassVisitor(int api, ClassVisitor classVisitor, ClassTweaker classTweaker, @Nullable BiConsumer<String, byte[]> generatedClassConsumer) {
		super(api, classVisitor);
		this.classTweaker = classTweaker;
		this.generatedClassConsumer = generatedClassConsumer;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		this.version = version;

		if ((access & Opcodes.ACC_ENUM) != 0) {
			enumExtensions = new HashMap<>(classTweaker.getEnumExtensions(name));
			generateOverrides = enumExtensions.values().stream().anyMatch(enumExtension -> !enumExtension.getMethodOverrides().isEmpty());
		}

		if (generateOverrides) {
			access &= ~Opcodes.ACC_FINAL;
		}

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (!enumExtensions.isEmpty()) {
			if ((access & Opcodes.ACC_ENUM) != 0) {
				return new EnumFieldVisitor(
					super.visitField(access, name, descriptor, signature, value),
					name
				);
			} else if (!wroteEnumFields) {
				// Write the enum fields before the first none enum field
				for (EnumExtension extension : enumExtensions.values()) {
					FieldVisitor fieldVisitor = super.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC + Opcodes.ACC_ENUM, extension.getName(), 'L' + className + ';', null, null);

					// Inject an annotation with some data so we can safely re-apply to the same class later.
					AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(ANNOTATION_DESC, false);
					annotationVisitor.visit("id", extension.getId());
					annotationVisitor.visit("hashCode", extension.hashCode());
				}

				wroteEnumFields = true;
			}
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		methods.add(new EntryTriple(className, name, descriptor));

		if (generateOverrides && version < Opcodes.V11 && "<init>".equals(name)) {
			// Open up constructor access.
			access = AccessUtils.makeProtected(access);
		}

		MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);

		if (!enumExtensions.isEmpty()) {
			boolean clinit = "<clinit>".equals(name) && "()V".equals(descriptor);

			if (clinit) {
				methodVisitor = new EnumClassInitializationMethodNode(methodVisitor, name, descriptor);
			}

			if ((version >= Opcodes.V15 && "$values".equals(name)) || (version < Opcodes.V15 && clinit)) {
				methodVisitor = new EnumArrayCreationMethodNode(methodVisitor, name, descriptor);
			}
		}

		return methodVisitor;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		for (EnumExtension extension : enumExtensions.values()) {
			if (extension.getMethodOverrides().isEmpty()) continue;

			if (generatedClassConsumer == null) {
				throw error("Cannot generate enum inner class as generatedClassConsumer is null");
			}

			// Generate any inner classes we may need
			final String innerName = Objects.requireNonNull(innerClassNames.get(extension.getName()), "Failed to find innerclass name for " + extension.getName());

			super.visitInnerClass(innerName, null, null, Opcodes.ACC_FINAL | Opcodes.ACC_ENUM);

			if (version >= Opcodes.V11) {
				visitNestMember(innerName);
			}

			// TODO what java version added this?
			visitPermittedSubclass(innerName);

			final EnumInnerClassWriter classWriter = new EnumInnerClassWriter(extension, innerName);
			classWriter.visit();
			generatedClassConsumer.accept(innerName, classWriter.toByteArray());
		}
	}

	private int computeMaxStack(int maxStack) {
		// TODO im not 100% sure on this.
		for (EnumExtension extension : enumExtensions.values()) {
			final Type constructor = extension.getTargetConstructor();
			int size = 4;

			for (Type parameter : constructor.getArgumentTypes()) {
				size += parameter.getSize();
			}

			maxStack = Math.max(maxStack, size);
		}

		return maxStack;
	}

	public RuntimeException error(String message, Object... args) {
		return new RuntimeException(String.format(message, args));
	}

	private final class EnumArrayCreationMethodNode extends MethodNode {
		private final MethodVisitor delegate;
		private final String methodName;
		private final String methodDesc;

		private EnumArrayCreationMethodNode(MethodVisitor delegate, String methodName, String methodDesc) {
			super(EnumExtensionClassVisitor.this.api);
			this.delegate = delegate;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		private InsnList entries(int size) {
			if (size < 0) {
				throw new IllegalStateException();
			}

			final InsnListGeneratorAdapter generatorAdapter = new InsnListGeneratorAdapter(EnumExtensionClassVisitor.this.api, 0, methodName, methodDesc);
			int i = 0;

			for (EnumExtension value : enumExtensions.values()) {
				int index = size + i;

				generatorAdapter.dup();
				generatorAdapter.push(index);
				generatorAdapter.getStatic(Type.getObjectType(className), value.getName(), Type.getObjectType(className));
				generatorAdapter.visitInsn(Opcodes.AASTORE);

				i++;
			}

			return generatorAdapter.getInsnList();
		}

		private void transform() {
			int size = -1;

			// Transform the array size.
			for (ListIterator<AbstractInsnNode> it = instructions.iterator(); it.hasNext();) {
				final AbstractInsnNode insn = it.next();

				if (insn.getType() == AbstractInsnNode.TYPE_INSN && insn.getOpcode() == Opcodes.ANEWARRAY) {
					final AbstractInsnNode arraySize = it.previous().getPrevious();
					assert insn == it.next(); // We went back, go forward again.

					if (AsmUtils.isIntConst(arraySize.getOpcode())) {
						size = arraySize.getOpcode() - Opcodes.ICONST_0;
					} else if (arraySize instanceof IntInsnNode) {
						size = ((IntInsnNode) arraySize).operand;
					}

					if (size == -1) {
						throw new UnsupportedOperationException();
					}

					instructions.remove(arraySize);
					instructions.insertBefore(insn, new IntInsnNode(Opcodes.BIPUSH, size + enumExtensions.size()));
				} else if (version < Opcodes.V15 && insn.getOpcode() == Opcodes.PUTSTATIC) {
					FieldInsnNode fieldInsnNode = ((FieldInsnNode) insn);

					if ("$VALUES".equals(fieldInsnNode.name)) {
						instructions.insertBefore(insn, entries(size));
					}
				} else if (version >= Opcodes.V15 && insn.getOpcode() == Opcodes.ARETURN) {
					instructions.insertBefore(insn, entries(size));
				}
			}
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(computeMaxStack(maxStack), maxLocals);
		}

		@Override
		public void visitEnd() {
			// Wait for the method node to be fully read, transform, and then pass on-to the delegate
			transform();
			accept(delegate);
		}
	}

	private class EnumClassInitializationMethodNode extends MethodNode {
		private final MethodVisitor delegate;
		private final String methodName;
		private final String methodDesc;

		private EnumClassInitializationMethodNode(MethodVisitor delegate, String methodName, String methodDesc) {
			super(EnumExtensionClassVisitor.this.api);
			this.delegate = delegate;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		private void transform() {
			int size = 0;

			for (ListIterator<AbstractInsnNode> it = instructions.iterator(); it.hasNext();) {
				final AbstractInsnNode insn = it.next();

				if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

					if ("<init>".equals(methodInsnNode.name)) {
						size++;
					}
				} else if (version >= Opcodes.V15 && insn.getOpcode() == Opcodes.INVOKESTATIC) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

					if ("$values".equals(methodInsnNode.name)) {
						instructions.insertBefore(insn, initFields(size));
						break;
					}
				} else if (version < Opcodes.V15 && insn.getOpcode() == Opcodes.ANEWARRAY) {
					// Insert before the array size
					instructions.insertBefore(it.previous().getPrevious(), initFields(size));
					break;
				}
			}
		}

		private InsnList initFields(int size) {
			final InsnListGeneratorAdapter generator = new InsnListGeneratorAdapter(EnumExtensionClassVisitor.this.api, 0, methodName, methodDesc);

			int i = 0;

			for (EnumExtension entry : enumExtensions.values()) {
				String targetClassName = className;

				if (!entry.getMethodOverrides().isEmpty()) {
					// Use the hashcode to provide a deterministic name for the anonymous class
					targetClassName = String.format("%s$%s$%d", className, entry.getId(), entry.getName().hashCode());
					innerClassNames.put(entry.getName(), targetClassName);
				}

				final Type enumClass = Type.getObjectType(targetClassName);

				generator.newInstance(enumClass);
				generator.dup();
				// Push name and index
				generator.push(entry.getName());
				generator.push(size + i++);

				final Type constructor = entry.getTargetConstructor();

				if (!methods.contains(new EntryTriple(className, "<init>", constructor.getDescriptor()))) {
					throw error("Could not find constructor with desc: " + constructor.getDescriptor());
				}

				if (constructor.getArgumentTypes().length > 2) {
					final EnumExtension.Parameters parameters = entry.getParameters();

					if (parameters == null) {
						String requiredParams = Arrays.stream(constructor.getArgumentTypes())
								.skip(2)
								.map(Type::getInternalName)
								.collect(Collectors.joining(","));

						throw error("No parameters provided for enum constructor, expected: [%s]", requiredParams);
					}

					if (parameters instanceof EnumExtension.ListParameters) {
						visitListParameters(generator, constructor, (EnumExtension.ListParameters) parameters);
					} else if (parameters instanceof EnumExtension.ConstantParameters) {
						visitConstantParameters(generator, (EnumExtension.ConstantParameters) parameters);
					} else {
						throw error("How did we get here?");
					}
				}

				// Invoke constructor and set our new field
				generator.invokeConstructor(enumClass, new Method("<init>", constructor.getDescriptor()));
				generator.putStatic(Type.getObjectType(className), entry.getName(), Type.getObjectType(className));
			}

			return generator.getInsnList();
		}

		private void visitListParameters(InsnListGeneratorAdapter generator, Type constructor, EnumExtension.ListParameters listParameters) {
			final Type[] argumentTypes = constructor.getArgumentTypes();

			for (int i = 2; i < argumentTypes.length; i++) {
				final EntryTriple listConstant = listParameters.getParamList();
				generator.getStatic(Type.getObjectType(listConstant.getOwner()), listConstant.getName(), Type.getType(listConstant.getDesc()));
				generator.push(i - 2);
				generator.invokeInterface(Type.getType(List.class), new Method("get", "(I)Ljava/lang/Object;"));
				generator.unbox(argumentTypes[i]); // TODO AsmUtils.getUnboxMethod might be better? Seems to decompile nicer
			}

			// TODO can we and do we want to validate the list size at runtime? We can even get the modid to it. Generate a synthetic method?
		}

		private void visitConstantParameters(InsnListGeneratorAdapter generator, EnumExtension.ConstantParameters constantParameters) {
			for (Object constant : constantParameters.getConstants()) {
				if (constant == null) {
					generator.push((String) null);
				} else if (constant instanceof String) {
					generator.push((String) constant);
				} else if (constant instanceof Character) {
					generator.push((char) constant);
				} else if (constant instanceof Byte) {
					generator.push((byte) constant);
				} else if (constant instanceof Short) {
					generator.push((short) constant);
				} else if (constant instanceof Integer) {
					generator.push((int) constant);
				} else if (constant instanceof Long) {
					generator.push((long) constant);
				} else if (constant instanceof Float) {
					generator.push((float) constant);
				} else if (constant instanceof Double) {
					generator.push((double) constant);
				} else if (constant instanceof Boolean) {
					generator.push((boolean) constant);
				} else {
					throw error("Unsupported constant type: " + constant.getClass());
				}
			}
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(computeMaxStack(maxStack), maxLocals);
		}

		@Override
		public void visitEnd() {
			transform();
			accept(delegate);
		}
	}

	private class EnumInnerClassWriter extends ClassWriter {
		private final EnumExtension extension;
		private final String innerName;

		EnumInnerClassWriter(EnumExtension extension, String innerName) {
			super(ClassWriter.COMPUTE_MAXS);
			this.extension = extension;
			this.innerName = innerName;
		}

		public void visit() {
			visit(version, Opcodes.ACC_FINAL | Opcodes.ACC_SUPER | Opcodes.ACC_ENUM, innerName, null, className, null);

			if (version >= Opcodes.V11) {
				visitNestHost(className);
			}

			visitOuterClass(className, null, null);
			visitInnerClass(innerName, null, null, Opcodes.ACC_FINAL | Opcodes.ACC_ENUM);

			visitConstructor();

			for (EnumExtension.MethodOverride override : extension.getMethodOverrides()) {
				visitOverride(override);
			}

			visitEnd();
		}

		private void visitConstructor() {
			final Method method = new Method("<init>", extension.getTargetConstructor().getDescriptor());
			final GeneratorAdapter generator = new GeneratorAdapter((version < Opcodes.V11 ? 0 : Opcodes.ACC_PRIVATE), method, null, null, this);

			generator.loadThis();
			generator.loadArgs();
			generator.invokeConstructor(Type.getObjectType(className), method);
			generator.returnValue();
			generator.endMethod();
		}

		private void visitOverride(EnumExtension.MethodOverride override) {
			final EntryTriple targetMethod = new EntryTriple(className, override.getTargetMethodName(), override.getStaticMethod().getDesc());

			// TODO do we need to allow overriding enum/object methods?
			// TODO check super method access
			if (!methods.contains(targetMethod)) {
				throw error("Unable to find method (%s) to override within enum from (%s)", targetMethod, extension.getId());
			}

			final Method method = new Method(override.getTargetMethodName(), override.getStaticMethod().getDesc());
			final GeneratorAdapter generator = new GeneratorAdapter(0, method, null, null, this);

			generator.loadArgs();
			generator.invokeStatic(Type.getObjectType(override.getStaticMethod().getOwner()), new Method(override.getStaticMethod().getName(), override.getStaticMethod().getDesc()));
			generator.returnValue();
			generator.endMethod();
		}
	}

	private class EnumFieldVisitor extends FieldVisitor {
		private final String name;
		private String id;
		private int hashcode;

		protected EnumFieldVisitor(FieldVisitor fieldVisitor, String name) {
			super(EnumExtensionClassVisitor.this.api, fieldVisitor);
			this.name = name;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (ANNOTATION_DESC.equals(descriptor)) {
				return new EnumFieldAnnotationVisitor(
					super.visitAnnotation(descriptor, visible)
				);
			}

			return super.visitAnnotation(descriptor, visible);
		}

		@Override
		public void visitEnd() {
			final EnumExtension extension = enumExtensions.get(name);

			if (extension != null) {
				if (!id.equals(extension.getId())) {
					throw error("Enum addition from (%s) clashes with (%s) in enum (%s) for entry (%s)", extension.getId(), id, className, name);
				}

				if (hashcode != extension.hashCode()) {
					throw error("Previously applied enum addition from (%s) does not match", id);
				}

				// Enum extension has already been applied to this class, safe to skip.
				enumExtensions.remove(name);
			}

			super.visitEnd();
		}

		private class EnumFieldAnnotationVisitor extends AnnotationVisitor {
			protected EnumFieldAnnotationVisitor(AnnotationVisitor annotationVisitor) {
				super(EnumExtensionClassVisitor.this.api, annotationVisitor);
			}

			@Override
			public void visit(String name, Object value) {
				switch (name) {
				case "id":
					id = (String) value;
					break;
				case "hashCode":
					hashcode = (int) value;
				}

				super.visit(name, value);
			}

			@Override
			public void visitEnd() {
				Objects.requireNonNull(id, String.format("Incomplete annotation on enum (%s) field (%s)", className, name));
				super.visitEnd();
			}
		}
	}
}
