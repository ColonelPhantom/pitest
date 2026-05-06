package com.infosupport.pitest;

import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.testapi.Configuration;
import org.pitest.testapi.TestGroupConfig;
import org.pitest.testapi.TestPluginFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public class LoggingTestPlugin implements TestPluginFactory {

    @Override
    public String name() {
        return "logging-wrapper";
    }

    @Override
    public String description() {
        return "Dynamically wraps the active test framework to add start/end boundaries";
    }

    @Override
    public Configuration createTestFrameworkConfiguration(TestGroupConfig config,
            ClassByteArraySource source, Collection<String> excludedRunners, Collection<String> includedTestMethods) {

        Configuration realConfig = StreamSupport.stream(
                ServiceLoader.load(TestPluginFactory.class).spliterator(), false)
            .filter(factory -> !factory.name().equals(this.name()))
            .map(factory -> factory.createTestFrameworkConfiguration(config, source, excludedRunners, includedTestMethods))
            .filter(c -> c != null && !c.getClass().getSimpleName().equals("NullConfiguration"))
            .max(Comparator.comparingInt(Configuration::priority))
            .orElse(null);

        if (realConfig == null) {
            return null; // Return null intentionally if no valid runner is found
        }

        return new LoggingConfiguration(realConfig);
    }
}

