package com.infosupport.pitest;

import org.pitest.bytecode.ASMVersion;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.commons.AdviceAdapter;


public class InstrumentationMethodVisitor extends AdviceAdapter {
    private String clazz;
    private String method;
    private static final String LOGGER_OWNER = "com/infosupport/pitest/Logger";

    private static final String LOGGER_CALL_METHOD_NAME = "logCall";
    private static final String LOGGER_CALL_METHOD_DESC = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V";

    private static final String LOGGER_RETURN_METHOD_NAME = "logReturn";
    private static final String LOGGER_RETURN_METHOD_DESC = "(Ljava/lang/Object;Ljava/lang/Object;)V";

    private static final String LOGGER_EXCEPTION_METHOD_NAME = "logException";
    private static final String LOGGER_EXCEPTION_METHOD_DESC = "(Ljava/lang/Object;Ljava/lang/Throwable;)V";

    private Label startLabel = new Label();
    private Label endLabel = new Label();
    private Label handlerLabel = new Label();

    private boolean methodEntered = false;

    public InstrumentationMethodVisitor(MethodVisitor methodVisitor, String clazz, int access, String method, String descriptor) {
        super(ASMVersion.ASM_VERSION, methodVisitor, access, method, descriptor);
        this.clazz = clazz;
        this.method = method;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        super.visitTryCatchBlock(startLabel, endLabel, handlerLabel, "java/lang/Throwable");
    }

    @Override
    protected void onMethodEnter() {
        System.out.println("onMethodEnter - " + clazz + "::" + method);
        methodEntered = true;
        super.visitLdcInsn(clazz);
        super.visitLdcInsn(method);
        if ((this.getAccess() & Opcodes.ACC_STATIC) != 0) {
            super.visitInsn(Opcodes.ACONST_NULL);
        } else {
            super.visitVarInsn(Opcodes.ALOAD, 0);
        }
        super.loadArgArray();
        super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_OWNER, LOGGER_CALL_METHOD_NAME, LOGGER_CALL_METHOD_DESC, false);

        super.visitLabel(startLabel);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != Opcodes.ATHROW) {
            if (opcode == Opcodes.RETURN) {
                super.visitLdcInsn(clazz);
                super.visitLdcInsn(method);
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_OWNER, LOGGER_RETURN_METHOD_NAME, LOGGER_RETURN_METHOD_DESC, false);
            } else {
                if (opcode == Opcodes.LRETURN || opcode == Opcodes.DRETURN) {
                    super.dup2();
                } else {
                    super.dup();
                }
                super.box(Type.getReturnType(methodDesc));

                if ((this.getAccess() & Opcodes.ACC_STATIC) != 0) {
                    super.visitInsn(Opcodes.ACONST_NULL);
                } else {
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                }
                super.swap();

                super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_OWNER, LOGGER_RETURN_METHOD_NAME, LOGGER_RETURN_METHOD_DESC, false);
            }
        }
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (!methodEntered) {
            super.visitLabel(startLabel);
        }
        mv.visitLabel(endLabel);
        mv.visitLabel(handlerLabel);
        mv.visitInsn(Opcodes.DUP);

        if ((this.getAccess() & Opcodes.ACC_STATIC) != 0) {
            super.visitInsn(Opcodes.ACONST_NULL);
        } else {
            super.visitVarInsn(Opcodes.ALOAD, 0);
        }
        super.swap();

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_OWNER, LOGGER_EXCEPTION_METHOD_NAME, LOGGER_EXCEPTION_METHOD_DESC, false);
        mv.visitInsn(Opcodes.ATHROW);

        super.visitMaxs(maxStack, maxLocals);
    }
}
