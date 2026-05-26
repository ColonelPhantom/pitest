package com.infosupport.pitest.carve;

import com.infosupport.pitest.MutantHash;
import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.ListenerArguments;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.MutationResultListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Map;
import com.infosupport.pitest.carve.LogParser.TestRun;
import com.infosupport.pitest.carve.LogParser.MethodCall;

public class TestCarver implements MutationResultListener {
    private static final String CARVE_DIR = "target/pit-instrument/";
    private static final File logFile = new File(CARVE_DIR, "carve.log");
    private static final FileOutputStream log;
    private final XStream xstream;
    private final Map<String, TestRun> cleanRuns;
    private final LogParser logParser;

    static {
        try {
            log = new FileOutputStream(logFile, false);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public TestCarver(ListenerArguments args) {
        this.xstream = new XStream();
        this.xstream.addPermission(AnyTypePermission.ANY);

        try {
            Collection<String> elements = args.data().getClassPathElements();
            URL[] urls = new URL[elements.size()];
            int i = 0;
            for (String element : elements) {
                urls[i++] = new File(element).toURI().toURL();
            }

            ClassLoader sutClassLoader = new URLClassLoader(urls, getClass().getClassLoader());
            this.xstream.setClassLoader(sutClassLoader);

            this.logParser = new LogParser(this.xstream);
            this.cleanRuns = logParser.parseLogFile(new File(CARVE_DIR, "clean-log.xml"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct SUT ClassLoader for XStream", e);
        }
    }

    @Override
    public void runStart() {
        try {
            log.write(("--- Original Clean Calls ---\n").getBytes());
            for (Map.Entry<String, TestRun> entry : cleanRuns.entrySet()) {
                log.write(("  Clean Test: " + entry.getKey() + "\n").getBytes());
                for (MethodCall call : entry.getValue().calls) {
                    log.write(("    Clean MethodCall: " + call.className + "::" + call.methodName + "\n").getBytes());
                    log.write(("      self: " + call.self + "\n").getBytes());
                    for (int i = 0; i < call.args.size(); i++) {
                        log.write(("      arg " + i + ": " + call.args.get(i) + "\n").getBytes());
                    }
                    if (call.returnValue != null) {
                        log.write(("      return: " + call.returnValue + "\n").getBytes());
                    }
                    if (call.exception != null) {
                        log.write(("      except: " + call.exception + "\n").getBytes());
                    }
                    if (call.selfAfter != null) {
                        log.write(("      selfAfter: " + call.selfAfter + "\n").getBytes());
                    }
                }
            }
            log.write(("----------------------------\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleMutationResult(ClassMutationResults results) {
        for (MutationResult result : results.getMutations()) {
            try {
                log.write((result.getDetails().getClassName() + "::" + result.getDetails().getMethod() + "\n").getBytes());
                log.write(("  " + result.getStatus().toString() + "\n").getBytes());
                if (!result.getStatus().hasCoverage() || !result.getSurvived()) {
                    continue;
                }

                log.write(("  Covering tests:" + "\n").getBytes());
                for (String test : result.getCoveringTests()) {
                    log.write(("    " + test + "\n").getBytes());
                }

                String id = MutantHash.hash(result.getDetails());
                File xmlFile = new File("target/pit-instrument/mutants/" + id + "/instrument-log.xml");

                if (!xmlFile.exists()) {
                    throw new RuntimeException("Expected log file does not exist: " + xmlFile.getAbsolutePath());
                }

                Map<String, TestRun> mutantRuns = logParser.parseLogFile(xmlFile);
                log.write(("  Parsed XML Logs for mutant: " + id + "\n").getBytes());

                for (Map.Entry<String, TestRun> entry : mutantRuns.entrySet()) {
                    String testName = entry.getKey();
                    TestRun mutantTestRun = entry.getValue();
                    TestRun cleanTestRun = this.cleanRuns.get(testName);

                    log.write(("    Test: " + testName + "\n").getBytes());

                    if (cleanTestRun == null) {
                        log.write(("      No corresponding clean test run found.\n").getBytes());
                        continue;
                    }

                    for (MethodCall mutantCall : mutantTestRun.calls) {
                        log.write(("      Mutant MethodCall: " + mutantCall.className + "::" + mutantCall.methodName + "\n").getBytes());
                        log.write(("        self: " + mutantCall.self + "\n").getBytes());
                        for (int i = 0; i < mutantCall.args.size(); i++) {
                            log.write(("        arg " + i + ": " + mutantCall.args.get(i) + "\n").getBytes());
                        }
                        if (mutantCall.returnValue != null) {
                            log.write(("        return: " + mutantCall.returnValue + "\n").getBytes());
                        }
                        if (mutantCall.exception != null) {
                            log.write(("        except: " + mutantCall.exception + "\n").getBytes());
                        }
                        if (mutantCall.selfAfter != null) {
                            log.write(("        selfAfter: " + mutantCall.selfAfter + "\n").getBytes());
                        }

                        boolean found = false;
                        for (MethodCall cleanCall : cleanTestRun.calls) {
                            if (mutantCall.matchesInput(cleanCall)) {
                                boolean distinguishable = mutantCall.distinguishable(cleanCall);
                                log.write(("        Found matching clean call. Distinguishable? " + distinguishable + "\t" + cleanCall + "\n").getBytes());                                found = true;
                            }
                        }
                        if (!found) {
                            log.write(("        No matching clean call found.\n").getBytes());
                        }
                    }
                }

            } catch (Exception e) {
                try {
                    log.write(("  Error processing mutant: " + e + "(" + e.getMessage() + ")\n").getBytes());
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            }
        }
    }

    @Override
    public void runEnd() {

    }
}
