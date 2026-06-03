package org.pitest.mutationtest.environment;

import org.pitest.plugin.ClientClasspathPlugin;

import java.lang.instrument.ClassFileTransformer;
import java.util.function.Predicate;

public interface TransformationPlugin extends ClientClasspathPlugin {

    @Deprecated
    default ClassFileTransformer makeTransformer() {
        return makeMutationTransformer(null);
    }

    default ClassFileTransformer makeCoverageTransformer(Predicate<String> sutFilter) {
        return null;
    }

    default ClassFileTransformer makeMutationTransformer(Predicate<String> sutFilter) {
        return null;
    }

    /**
     * Called once the coverage minion has finished executing all tests.
     */
    default void coverageFinished() {
        // default no-op
    }

}
