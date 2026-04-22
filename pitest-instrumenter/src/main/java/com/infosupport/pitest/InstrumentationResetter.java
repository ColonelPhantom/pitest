package com.infosupport.pitest;

import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.environment.ResetEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Predicate;

public class InstrumentationResetter implements ResetEnvironment {
    private InstrumentationTransformer instrumenter;

    public InstrumentationResetter(InstrumentationTransformer instrumenter) {
        this.instrumenter = instrumenter;
    }

    @Override
    public void resetFor(Mutant mutatedClass) {
        String clazz = mutatedClass.getDetails().getClassName().asJavaName();
        String method = mutatedClass.getDetails().getMethod();
        instrumenter.setMethod(Predicate.isEqual(clazz), method);

        MutationIdentifier id = mutatedClass.getDetails().getId();
        File file = new File("instrument-mutants/" + id.hashCode());
        try {
            FileOutputStream out = new FileOutputStream(file, false);
            out.write(id.toString().getBytes());
            out.write("\n".getBytes());
            Logger.setOutput(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
