package com.infosupport.pitest.trace;

import com.infosupport.pitest.Logger;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;
import org.pitest.testapi.TestUnit;

public class LoggingTestUnit implements TestUnit {
    private final TestUnit delegate;

    public LoggingTestUnit(TestUnit delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(ResultCollector rc) {
        String testName = delegate.getDescription().getQualifiedName();
        Logger.startTest(testName);
        try {
            delegate.execute(rc);
        } finally {
            Logger.endTest();
        }
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }
}

