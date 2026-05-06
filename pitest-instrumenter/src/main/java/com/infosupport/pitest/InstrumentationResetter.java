package com.infosupport.pitest;

import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.environment.ResetEnvironment;

public class InstrumentationResetter implements ResetEnvironment {
    private InstrumentationTransformer instrumenter;

    public InstrumentationResetter(InstrumentationTransformer instrumenter) {
        this.instrumenter = instrumenter;
    }

    @Override
    public void resetFor(Mutant mutatedClass) {
        instrumenter.setMutant(mutatedClass);
    }
}
