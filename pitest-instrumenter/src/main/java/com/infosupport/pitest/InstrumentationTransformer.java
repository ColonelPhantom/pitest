package com.infosupport.pitest;

//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.ClassWriter;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.ClassWriter;


import java.io.File;
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
//            System.out.println("Not initialized yet; skipping " + className);
            return null;
        }
        if (!classFilter.test(className.replace("/", "."))) {
//            System.out.println("Skipping class " + className);
            return null;
        }
        if (transformedClasses.contains(ByteBuffer.wrap(classfileBuffer).asReadOnlyBuffer())) {
            System.out.println("Already transformed class " + className);
            return null;
        }

        System.out.println("Transforming class " + className);

        byte[] result;
        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new InstrumentationClassVisitor(cw, methodFilter);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            result = cw.toByteArray();
            transformedClasses.add(ByteBuffer.wrap(result).asReadOnlyBuffer());
            System.out.println("Before: " + Arrays.hashCode(classfileBuffer) + ", after: " + Arrays.hashCode(result));
        } catch (Throwable e) {
            System.out.println("Transforming class " + className + " failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        File outputDir = new File("instrumented-classes");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        try {
            FileOutputStream out = new FileOutputStream(outputDir + "/" + className.replace("/", ".") + "-" + Arrays.hashCode(classfileBuffer) + ".class", false);
            out.write(result);
        } catch (IOException e) {
            System.out.println("Failed to write transformed class " + e);
        }
        System.out.println("Transformed class " + className + " - size "  + result.length);
        return result;
    }
}
