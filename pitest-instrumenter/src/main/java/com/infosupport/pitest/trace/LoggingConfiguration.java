package com.infosupport.pitest.trace;

import org.pitest.help.PitHelpError;
import org.pitest.testapi.Configuration;
import org.pitest.testapi.TestSuiteFinder;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitFinder;
import org.pitest.testapi.TestUnitExecutionListener;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LoggingConfiguration implements Configuration {
    private final Configuration delegate;

    public LoggingConfiguration(Configuration delegate) {
        this.delegate = delegate;
    }

    @Override
    public int priority() {
        // Lower number means higher priority. Ensure we execute before the delegate.
        return delegate.priority() - 10;
    }

    @Override
    public TestUnitFinder testUnitFinder() {
        TestUnitFinder realFinder = delegate.testUnitFinder();
        return new TestUnitFinder() {
            @Override
            public List<TestUnit> findTestUnits(Class<?> clazz, TestUnitExecutionListener listener) {
                return realFinder.findTestUnits(clazz, listener).stream()
                        .map(LoggingTestUnit::new)
                        .collect(Collectors.toList());
            }
        };
    }

    @Override
    public TestSuiteFinder testSuiteFinder() {
        return delegate.testSuiteFinder();
    }

    @Override
    public Optional<PitHelpError> verifyEnvironment() {
        return delegate.verifyEnvironment();
    }
}

