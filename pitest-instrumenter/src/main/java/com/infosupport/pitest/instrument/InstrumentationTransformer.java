package com.infosupport.pitest.instrument;

import com.infosupport.pitest.Logger;
import com.infosupport.pitest.MutantHash;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.ClassWriter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.function.Predicate;

class InstrumentationTransformer implements ClassFileTransformer {
    private Predicate<String> classFilter;
    private String methodFilter;
    private String mutantId;

    private String basePath() {
        if (this.mutantId != null) {
            return "target/pit-instrument/mutants/" + this.mutantId + "/";
        } else {
            return "target/pit-instrument/cov-classes/";
        }
    }

    public InstrumentationTransformer(Predicate<String> sutFilter) {
        this.classFilter = sutFilter;
    }

    public void setMutant(Mutant newMutant) {
        // collect all metadata
        MutationDetails details = newMutant.getDetails();
        String clazz = details.getClassName().asJavaName();
        String method = details.getMethod();

        // set filters
        this.classFilter = Predicate.isEqual(clazz);
        this.methodFilter = method;

        System.err.println("Setting mutant in " + clazz + "::" + method);

        // calculate hash
        this.mutantId = MutantHash.hash(details);

        System.err.println("idStr: " + this.mutantId + " (" + MutantHash.idString(details) + ")\n");

        // create output files
        InstrumentationPlugin.mkdir("mutants/" + this.mutantId);
        try {
            File logFile = new File(basePath() + "instrument-log.xml.gz");
            FileOutputStream out = new FileOutputStream(logFile, false);
            Logger.setOutput(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        try {
            if (classFilter == null || !classFilter.test(className.replace("/", "."))) {
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
                System.out.println("Before: " + Arrays.hashCode(classfileBuffer) + ", after: " + Arrays.hashCode(result));
            } catch (Throwable e) {
                System.out.println("Transforming class " + className + " failed: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            try {
                FileOutputStream out = new FileOutputStream(basePath() + className.replace("/", ".") + ".class", false);
                out.write(result);
                out.close();
            } catch (IOException e) {
                System.out.println("Failed to write transformed class " + e);
            }
            System.out.println("Transformed class " + className + " - size " + result.length);
            return result;
        } catch (Throwable e) {
            System.out.println("Transforming class " + className + " failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
