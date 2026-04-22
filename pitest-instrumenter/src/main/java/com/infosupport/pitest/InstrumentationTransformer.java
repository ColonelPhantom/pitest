package com.infosupport.pitest;

//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.ClassWriter;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.ClassWriter;


import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

class InstrumentationTransformer implements ClassFileTransformer {
    private boolean isMutant = false;
    private Predicate<String> classFilter;
    private String methodFilter;
    private Set<ByteBuffer> transformedClasses;

    public InstrumentationTransformer(boolean isMutant, Predicate<String> sutFilter) {
        this.isMutant = isMutant;
        this.classFilter = sutFilter;
        transformedClasses = new HashSet<>();
    }

    public void setMethod(Predicate<String> classFilter, String methodFilter) {
        this.classFilter = classFilter;
        this.methodFilter = methodFilter;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (classFilter == null) {
            System.out.println("Not initialized yet; skipping " + className);
            return null;
        }
        if (!classFilter.test(className.replace("/", "."))) {
            System.out.println("Skipping class " + className);
            return null;
        }
        if (transformedClasses.contains(ByteBuffer.wrap(classfileBuffer).asReadOnlyBuffer())) {
            System.out.println("Already transformed class " + className);
            return null;
        }

        try {
            String filename = isMutant ? "pitest-instrumenter-mut.log" : "pitest-instrumenter.log";
            FileOutputStream output = new FileOutputStream(filename, true);
            output.write(className.getBytes());
            output.write("\n".getBytes());
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Transforming class " + className);

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new InstrumentationClassVisitor(cw, methodFilter);
        cr.accept(cv, 0);
        byte[] result = cw.toByteArray();
        transformedClasses.add(ByteBuffer.wrap(result).asReadOnlyBuffer());
        System.out.println("Before: " + Arrays.hashCode(classfileBuffer) + ", after: " + Arrays.hashCode(result));
        return result;
    }
}
