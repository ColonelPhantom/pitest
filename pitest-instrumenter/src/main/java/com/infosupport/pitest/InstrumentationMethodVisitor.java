package com.infosupport.pitest;

import org.pitest.bytecode.ASMVersion;
import org.pitest.reloc.asm.MethodVisitor;

public class InstrumentationMethodVisitor extends MethodVisitor {
    public InstrumentationMethodVisitor() {
        super(ASMVersion.ASM_VERSION);
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }
}
