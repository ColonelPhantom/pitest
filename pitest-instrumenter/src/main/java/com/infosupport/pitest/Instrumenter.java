package com.infosupport.pitest;

import org.pitest.mutationtest.environment.TransformationPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

class NullClassFileTransformer implements ClassFileTransformer {
    private boolean isMutant = false;

    public NullClassFileTransformer(boolean isMutant) {
        this.isMutant = isMutant;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
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
        return null;
    }
}

public class Instrumenter implements TransformationPlugin {
    @Override
    public String description() {
        return "Instruments the SUT to log all method invocations";
    }

    @Override
    public ClassFileTransformer makeCoverageTransformer() {
//        throw new UnsupportedOperationException("Coverage transformation is not supported");
//        File dir = new File("pitest-instrumenter");
//        if (!dir.exists()) { dir.mkdir(); }
//        if (!dir.isDirectory()) {
//            throw new RuntimeException("Failed to create directory for instrumented classes");
//        }
        File oldlog = new File("pitest-instrumenter.log");
        if (oldlog.exists()) { oldlog.delete(); }
        return new NullClassFileTransformer(false);
    }

    @Override
    public ClassFileTransformer makeMutationTransformer() {
//        throw new UnsupportedOperationException("Mutation transformation is not supported");
//        File dir = new File("pitest-instrumenter-mut");
//        if (!dir.exists()) { dir.mkdir(); }
//        if (!dir.isDirectory()) {
//            throw new RuntimeException("Failed to create directory for instrumented classes");
//        }

        File oldlog = new File("pitest-instrumenter-mut.log");
        if (oldlog.exists()) { oldlog.delete(); }

        return new NullClassFileTransformer(true);
    }
}
