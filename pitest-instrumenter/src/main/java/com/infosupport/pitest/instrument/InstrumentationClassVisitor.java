package com.infosupport.pitest.instrument;

import org.pitest.bytecode.ASMVersion;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.ClassWriter;
import org.pitest.reloc.asm.MethodVisitor;

public class InstrumentationClassVisitor extends ClassVisitor {
    private String clazz;
    private final String methodToInstrument;

    public InstrumentationClassVisitor(ClassWriter cw, String methodToInstrument) {
        super(ASMVersion.ASM_VERSION, cw);
        this.methodToInstrument = methodToInstrument;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.clazz = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions);

        if (this.methodToInstrument == null || this.methodToInstrument.equals(name)) {
            return new InstrumentationMethodVisitor(methodVisitor, clazz, access, name, descriptor);
        } else {
            return methodVisitor;
        }
    }
}
