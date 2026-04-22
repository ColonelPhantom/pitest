package com.infosupport.pitest;

//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.ClassWriter;
//import org.objectweb.asm.MethodVisitor;
import org.pitest.bytecode.ASMVersion;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.ClassWriter;
import org.pitest.reloc.asm.MethodVisitor;

import static com.infosupport.pitest.Logger.logCall;

public class InstrumentationClassVisitor extends ClassVisitor {
    private String methodToInstrument;

    public InstrumentationClassVisitor(ClassWriter cw, String methodToInstrument) {
        super(ASMVersion.ASM_VERSION, cw);
        this.methodToInstrument = methodToInstrument;

        System.out.println("Instrumentation ClassVisitor: " + cw.getClass().getName() + "::" + methodToInstrument);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (this.methodToInstrument == null || this.methodToInstrument.equals(name)) {
            // There is no filter OR there is no equality check.
            logCall(this.getClass().getName(), name, null);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
