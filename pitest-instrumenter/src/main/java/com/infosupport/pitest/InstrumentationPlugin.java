package com.infosupport.pitest;

import org.pitest.mutationtest.environment.EnvironmentResetPlugin;
import org.pitest.mutationtest.environment.ResetEnvironment;
import org.pitest.mutationtest.environment.TransformationPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.util.function.Predicate;

public class InstrumentationPlugin implements TransformationPlugin, EnvironmentResetPlugin {
    private static InstrumentationTransformer instrumenter;

    private static Predicate<String> convertToJVMClassFilter(
            final Predicate<String> child) {
        return a -> child.test(a.replace("/", "."));
    }

    @Override
    public String description() {
        return "Instruments the SUT to log all method invocations";
    }

    @Override
    public ClassFileTransformer makeCoverageTransformer(Predicate<String> filter) {
        if (instrumenter != null) {
            throw new IllegalStateException("Trying to create two instrumenters in one JVM!");
        }
        // TODO: coverage transformer seems to instrument test classes as well?
        //  That's not what we want, but I don't see how to avoid it currently.
        //  However it doesn't matter too much; it mostly adds some bloat to the log file and isn't too impactful.
        if (filter == null) {
            throw new RuntimeException("Filter is not set. Make sure to call updateConfig before using the transformer");
        }

        try {
            Logger.setOutput(new FileOutputStream("instrumentation-clean.log", false));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        instrumenter = new InstrumentationTransformer(false, convertToJVMClassFilter(filter));
        return instrumenter;
    }

    @Override
    public ClassFileTransformer makeMutationTransformer(Predicate<String> filter) {
        if (instrumenter != null) {
            throw new IllegalStateException("Trying to create two instrumenters in one JVM!");
        }

        instrumenter = new InstrumentationTransformer(true, filter != null ? convertToJVMClassFilter(filter) : null);
        return instrumenter;
    }

    @Override
    public ResetEnvironment make() {
        if (instrumenter == null) {
            try {
                FileOutputStream error = null;
                error = new FileOutputStream("pitest-instrumenter-error.log", true);
                error.write("Trying to create resetter before instrumenter!".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new IllegalStateException("Trying to create resetter before instrumenter!");
        }

        File dir = new File("instrument-mutants");
        if (dir.exists()) {
            dir.delete();
        }
        dir.mkdir();
        return new InstrumentationResetter(instrumenter);
    }
}
