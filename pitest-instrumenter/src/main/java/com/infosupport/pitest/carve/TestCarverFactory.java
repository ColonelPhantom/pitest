package com.infosupport.pitest.carve;

import org.pitest.mutationtest.ListenerArguments;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.MutationResultListenerFactory;

import java.util.Properties;

public class TestCarverFactory implements MutationResultListenerFactory {
    @Override
    public MutationResultListener getListener(Properties props, ListenerArguments args) {
        return new TestCarver(args);
    }

    @Override
    public String name() {
        return "carver";
    }

    @Override
    public String description() {
        return "Carve tests from instrumentation results";
    }
}
