package com.infosupport.pitest;

import org.pitest.bytecode.ASMVersion;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;


public class InstrumentationMethodVisitor extends MethodVisitor {
    private String clazz;
    private String method;
    private static final String LOGGER_OWNER = "com/infosupport/pitest/Logger";
    private static final String LOGGER_METHOD_NAME = "logCall";
    private static final String LOGGER_METHOD_DESC = "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V";

    public InstrumentationMethodVisitor(MethodVisitor methodVisitor, String clazz, String method) {
        super(ASMVersion.ASM_VERSION, methodVisitor);
        this.clazz = clazz;
        this.method = method;
    }

    @Override
    public void visitCode() {
        System.out.println("visitCode - " + clazz + "::" + method);
        super.visitLdcInsn(clazz);
        super.visitLdcInsn(method);
        super.visitInsn(Opcodes.ACONST_NULL);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_OWNER, LOGGER_METHOD_NAME, LOGGER_METHOD_DESC, false);

        super.visitCode();
    }
}
