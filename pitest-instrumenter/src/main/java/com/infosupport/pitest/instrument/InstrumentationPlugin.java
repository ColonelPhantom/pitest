package com.infosupport.pitest.instrument;

import com.infosupport.pitest.Logger;
import org.pitest.mutationtest.environment.EnvironmentResetPlugin;
import org.pitest.mutationtest.environment.ResetEnvironment;
import org.pitest.mutationtest.environment.TransformationPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    protected static void mkdir(String dirname) {
        File dir = new File("target/pit-instrument/" + dirname);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (!result) {
                throw new RuntimeException("Cannot create directory " + dirname);
            }
        }
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

        mkdir("cov-classes");

        try {
            Logger.setOutput(new FileOutputStream("target/pit-instrument/clean-log.xml", false));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        instrumenter = new InstrumentationTransformer(convertToJVMClassFilter(filter));
        return instrumenter;
    }

    @Override
    public ClassFileTransformer makeMutationTransformer(Predicate<String> filter) {
        if (instrumenter != null) {
            throw new IllegalStateException("Trying to create two instrumenters in one JVM!");
        }

        mkdir("mutants");
        instrumenter = new InstrumentationTransformer(filter != null ? convertToJVMClassFilter(filter) : null);
        return instrumenter;
    }

    @Override
    public ResetEnvironment make() {
        if (instrumenter == null) {
            throw new IllegalStateException("Trying to create resetter before instrumenter!");
        }

        return new InstrumentationResetter(instrumenter);
    }
}
