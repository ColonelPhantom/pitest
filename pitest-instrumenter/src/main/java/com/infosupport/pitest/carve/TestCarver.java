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
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import com.infosupport.pitest.carve.LogParser.TestRun;
import com.infosupport.pitest.carve.LogParser.MethodCall;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeName;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;

import javax.lang.model.element.Modifier;

public class TestCarver implements MutationResultListener {
    private static final String CARVE_DIR = "target/pit-instrument/";
    private static final File logFile = new File(CARVE_DIR, "carve.log");
    private static final FileOutputStream log;
    private final XStream xstream;
    private final Map<String, TestRun> cleanRuns;
    private final LogParser logParser;
    private final ClassLoader sutClassLoader;

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
            this.sutClassLoader = sutClassLoader;
            this.xstream.setClassLoader(sutClassLoader);
            this.xstream.ignoreUnknownElements();
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct SUT ClassLoader for XStream", e);
        }

        this.logParser = new LogParser(this.xstream);
        this.cleanRuns = logParser.parseLogFile(new File(CARVE_DIR, "clean-log.xml.gz"));
    }

    @Override
    public void runStart() {
//        try {
//            log.write(("--- Original Clean Calls ---\n").getBytes());
//            for (Map.Entry<String, TestRun> entry : cleanRuns.entrySet()) {
//                log.write(("  Clean Test: " + entry.getKey() + "\n").getBytes());
//                for (MethodCall call : entry.getValue().calls) {
//                    log.write(("    Clean MethodCall: " + call.className + "::" + call.methodName + "\n").getBytes());
//                    log.write(("      self: " + call.self + "\n").getBytes());
//                    for (int i = 0; i < call.args.size(); i++) {
//                        log.write(("      arg " + i + ": " + call.args.get(i) + "\n").getBytes());
//                    }
//                    if (call.returnValue != null) {
//                        log.write(("      return: " + call.returnValue + "\n").getBytes());
//                    }
//                    if (call.exception != null) {
//                        log.write(("      except: " + call.exception + "\n").getBytes());
//                    }
//                    if (call.selfAfter != null) {
//                        log.write(("      selfAfter: " + call.selfAfter + "\n").getBytes());
//                    }
//                }
//            }
//            log.write(("----------------------------\n").getBytes());
//        } catch (Throwable t) {
//            try {
//                log.write(("Error writing clean calls to log: " + t.getMessage() + "\n").getBytes());
//                log.write(("----------------------------\n").getBytes());
//            } catch (IOException ignored) {}
//        }
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
                String details = MutantHash.idString(result.getDetails());
                log.write(("  Mutant ID: " + details + "\n").getBytes());

                ArrayList<File> xmlFiles = new ArrayList<>();

                File xmlFile = new File("target/pit-instrument/mutants/" + id + "/instrument-log.xml.gz");
                if (xmlFile.exists()) {
                    xmlFiles.add(xmlFile);
                } else {
                    for (Integer index : result.getDetails().getId().getIndexes()) {
                        MutationIdentifier subId = new MutationIdentifier(
                                result.getDetails().getId().getLocation(),
                                index,
                                result.getDetails().getId().getMutator()
                        );
                        MutationDetails subDetails = new MutationDetails(
                                subId,
                                result.getDetails().getFilename(),
                                result.getDetails().getDescription(),
                                result.getDetails().getLineNumber(),
                                result.getDetails().getBlocks()
                        );
                        String subIdStr = MutantHash.idString(subDetails);
                        String subHash = MutantHash.hash(subDetails);
                        File subXmlFile = new File("target/pit-instrument/mutants/" + subHash + "/instrument-log.xml.gz");
                        if (subXmlFile.exists()) {
                            xmlFiles.add(subXmlFile);
                        } else {
                            log.write(("  Warning: No XML log found for mutant or sub-mutant: " + subIdStr + " / " + subHash + "\n").getBytes());
                        }
                    }
                }

                Map<String, TestRun> mutantRuns = logParser.parseLogFiles(xmlFiles);
                log.write(("  Parsed XML Logs for mutant: " + id + "\n").getBytes());

                tests: for (Map.Entry<String, TestRun> entry : mutantRuns.entrySet()) {
                    String testName = entry.getKey();
                    TestRun mutantTestRun = entry.getValue();
                    TestRun cleanTestRun = this.cleanRuns.get(testName);

                    log.write(("    Test: " + testName + "\n").getBytes());

                    if (cleanTestRun == null) {
                        log.write(("      No corresponding clean test run found.\n").getBytes());
                        continue;
                    }

                    for (Map.Entry<String, Map<String, List<MethodCall>>> mutantCallForClass : mutantTestRun.calls.entrySet()) {
                        String className = mutantCallForClass.getKey();
                        Map<String, List<MethodCall>> methodCalls = mutantCallForClass.getValue();
                        for (Map.Entry<String, List<MethodCall>> mutantCallEntry : methodCalls.entrySet()) {
                            String methodName = mutantCallEntry.getKey();
                            List<MethodCall> mutantCalls = mutantCallEntry.getValue();
                            for (MethodCall mutantCall : mutantCalls) {
                                int callIndex = 0;
                                for (MethodCall cleanCall : cleanTestRun.calls.get(className).get(methodName)) {
                                    callIndex++;
                                    if (mutantCall.matchesInput(cleanCall)) {
                                        boolean distinguishable = mutantCall.distinguishable(cleanCall);
                                        if (distinguishable) {
                                            generateTest(mutantCall, cleanCall, id, testName, callIndex);
                                            break tests; // Only generate one test per mutant
                                        }
                                    }
                                }
                            }
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

    private void generateTest(MethodCall mutantCall, MethodCall cleanCall, String mutantId, String sourceTest, int callIndex) {
        try {
            String classNameDot = mutantCall.className.replace('/', '.');
            String packageName = "";
            String simpleClassName = classNameDot;
            int lastDot = classNameDot.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = classNameDot.substring(0, lastDot);
                simpleClassName = classNameDot.substring(lastDot + 1);
            }

            String camelClassName = toUpperCamelCase(simpleClassName);
            String camelMethodName = toUpperCamelCase(mutantCall.methodName);
            String camelSourceTest = toUpperCamelCase(sourceTest);
            String shortMutantId = mutantId.substring(0, 8);

            System.out.println("Generating test for mutant " + mutantId + " based on test " + sourceTest + " for method call " + classNameDot + "::" + mutantCall.methodName);

            String testClassName = String.format("%s%sFrom%sMutant%sCall%dTest",
                    camelClassName, camelMethodName, camelSourceTest, shortMutantId, callIndex);

            java.lang.reflect.Method targetMethod = resolveTargetMethod(classNameDot, mutantCall.methodName, cleanCall.args);
            if (targetMethod == null) {
                log.write(("        Warning: Could not resolve target method for " + classNameDot + "::" + mutantCall.methodName + "\n").getBytes());
                return;
            }
            Class<?>[] paramTypes = targetMethod.getParameterTypes();
            List<String> paramNames = extractParamNames(targetMethod);
            Class<?> returnType = targetMethod.getReturnType();

            boolean exceptionDiff = !deserializedEqual(cleanCall.exception, mutantCall.exception);
            boolean returnDiff = !deserializedEqual(cleanCall.returnValue, mutantCall.returnValue);
            boolean selfAfterDiff = !deserializedEqual(cleanCall.selfAfter, mutantCall.selfAfter);
            boolean returnsVoid = Void.TYPE.equals(returnType);

            MethodSpec testMethod;
            if (exceptionDiff && cleanCall.exception != null) {
                testMethod = generateTestException(mutantCall, cleanCall, classNameDot, paramTypes, paramNames);
            } else if (returnDiff && !returnsVoid) {
                testMethod = generateTestResult(mutantCall, cleanCall, classNameDot, paramTypes, paramNames, returnType);
            } else if (selfAfterDiff) {
                testMethod = generateTestSelf(mutantCall, cleanCall, classNameDot, paramTypes, paramNames);
            } else {
                testMethod = generateTestVoid(mutantCall, cleanCall, classNameDot, paramTypes, paramNames);
            }
            writeTestFile(testClassName, packageName, testMethod, sourceTest);
        } catch (Exception e) {
            e.printStackTrace();
            try { log.write(("        Failed to generate test: " + e.getMessage() + "\n").getBytes()); } catch(Exception ignored) {}
        }
    }

    private MethodSpec.Builder buildArrange(MethodCall cleanCall, String classNameDot, Class<?>[] paramTypes, List<String> paramNames, StringBuilder argsStr) throws Exception {
        MethodSpec.Builder mb = MethodSpec.methodBuilder("test")
                .addAnnotation(ClassName.get("org.junit.jupiter.api", "Test"))
                .addModifiers(Modifier.PUBLIC)
                .addException(Exception.class);

        mb.addComment("Arrange");

        if (cleanCall.self != null) {
            String selfXml = this.xstream.toXML(cleanCall.self.object);
            mb.addStatement("$T self = ($T) this.xstream.fromXML($S)", TypeName.get(Class.forName(classNameDot, true, this.sutClassLoader)), TypeName.get(Class.forName(classNameDot, true, this.sutClassLoader)), selfXml);
        }

        for (int i = 0; i < cleanCall.args.size(); i++) {
            String argXml = this.xstream.toXML(cleanCall.args.get(i).object);
            Class<?> paramType = i < paramTypes.length ? paramTypes[i] : null;
            TypeName argType = paramType != null
                    ? TypeName.get(paramType)
                    : (cleanCall.args.get(i).object != null ? TypeName.get(cleanCall.args.get(i).object.getClass()) : ClassName.get(Object.class));
            String argVarName = paramNames != null && i < paramNames.size() ? paramNames.get(i) : "arg" + i;
            mb.addStatement("$T $L = ($T) this.xstream.fromXML($S)", argType, argVarName, argType, argXml);
            if (i > 0) argsStr.append(", ");
            argsStr.append(argVarName);
        }
        return mb;
    }

    private void writeTestFile(String testClassName, String packageName, MethodSpec testMethod, String sourceTest) throws IOException {
        TypeSpec testClass = TypeSpec.classBuilder(testClassName)
                .addField(XStream.class, "xstream", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("this.xstream = new $T()", XStream.class)
                        .addStatement("this.xstream.addPermission($T.ANY)", com.thoughtworks.xstream.security.AnyTypePermission.class)
                        .build())
                .addMethod(testMethod)
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, testClass)
                .addFileComment("Source test: $L", sourceTest)
                .build();
        File outputDir = new File("target/pit-instrument/carved/");
        javaFile.writeTo(outputDir);
        log.write(("        Generated test at: " + new File(outputDir, packageName.replace('.', '/') + "/" + testClassName + ".java").getAbsolutePath() + "\n").getBytes());
    }

    private MethodSpec generateTestResult(MethodCall mutantCall, MethodCall cleanCall, String classNameDot, Class<?>[] paramTypes, List<String> paramNames, Class<?> returnType) throws Exception {
        StringBuilder argsStr = new StringBuilder();
        MethodSpec.Builder mb = buildArrange(cleanCall, classNameDot, paramTypes, paramNames, argsStr);

        mb.addCode("\n");
        mb.addComment("Act");
        String actualReturnVar = "actualReturn";
        TypeName returnTypeName = returnType != null
                ? TypeName.get(returnType)
                : (cleanCall.returnValue.object != null ? TypeName.get(cleanCall.returnValue.object.getClass()) : ClassName.get(Object.class));
        
        if (cleanCall.self != null) {
            mb.addStatement("$T $L = self.$L($L)", returnTypeName, actualReturnVar, mutantCall.methodName, argsStr.toString());
        } else {
            mb.addStatement("$T $L = $T.$L($L)", returnTypeName, actualReturnVar, ClassName.bestGuess(classNameDot), mutantCall.methodName, argsStr.toString());
        }

        mb.addCode("\n");
        mb.addComment("Assert");
        String expectedReturnXml = this.xstream.toXML(cleanCall.returnValue.object);
        mb.addStatement("$T expectedReturn = ($T) this.xstream.fromXML($S)", returnTypeName, returnTypeName, expectedReturnXml);
        mb.addStatement("$T.assertEquals(expectedReturn, $L)", ClassName.get("org.junit.jupiter.api", "Assertions"), actualReturnVar);

        return mb.build();
    }

    private MethodSpec generateTestSelf(MethodCall mutantCall, MethodCall cleanCall, String classNameDot, Class<?>[] paramTypes, List<String> paramNames) throws Exception {
        StringBuilder argsStr = new StringBuilder();
        MethodSpec.Builder mb = buildArrange(cleanCall, classNameDot, paramTypes, paramNames, argsStr);

        mb.addCode("\n");
        mb.addComment("Act");
        if (cleanCall.self != null) {
            mb.addStatement("self.$L($L)", mutantCall.methodName, argsStr.toString());
        } else {
            mb.addStatement("$T.$L($L)", ClassName.bestGuess(classNameDot), mutantCall.methodName, argsStr.toString());
        }

        mb.addCode("\n");
        mb.addComment("Assert");
        String expectedSelfAfterXml = this.xstream.toXML(cleanCall.selfAfter.object);
        mb.addStatement("$T expectedSelfAfter = ($T) this.xstream.fromXML($S)", TypeName.get(Class.forName(classNameDot, true, this.sutClassLoader)), TypeName.get(Class.forName(classNameDot, true, this.sutClassLoader)), expectedSelfAfterXml);
        mb.addStatement("$T.assertEquals(expectedSelfAfter, self)", ClassName.get("org.junit.jupiter.api", "Assertions"));

        return mb.build();
    }

    private MethodSpec generateTestException(MethodCall mutantCall, MethodCall cleanCall, String classNameDot, Class<?>[] paramTypes, List<String> paramNames) throws Exception {
        StringBuilder argsStr = new StringBuilder();
        MethodSpec.Builder mb = buildArrange(cleanCall, classNameDot, paramTypes, paramNames, argsStr);

        mb.addCode("\n");
        mb.addComment("Act & Assert");
        String expectedExceptionXml = this.xstream.toXML(cleanCall.exception.object);
        mb.addCode("try {\n");
        if (cleanCall.self != null) {
            mb.addStatement("  self.$L($L)", mutantCall.methodName, argsStr.toString());
        } else {
            mb.addStatement("  $T.$L($L)", ClassName.bestGuess(classNameDot), mutantCall.methodName, argsStr.toString());
        }
        mb.addStatement("  $T.fail($S)", ClassName.get("org.junit.jupiter.api", "Assertions"), "Expected Exception");
        mb.addCode("} catch ($T e) {\n", Throwable.class);
        mb.addStatement("  $T expectedException = ($T) this.xstream.fromXML($S)", Throwable.class, Throwable.class, expectedExceptionXml);
        mb.addStatement("  $T.assertEquals(expectedException.getClass(), e.getClass())", ClassName.get("org.junit.jupiter.api", "Assertions"));
        mb.addStatement("  $T.assertEquals(expectedException.getMessage(), e.getMessage())", ClassName.get("org.junit.jupiter.api", "Assertions"));
        mb.addCode("}\n");

        return mb.build();
    }

    private MethodSpec generateTestVoid(MethodCall mutantCall, MethodCall cleanCall, String classNameDot, Class<?>[] paramTypes, List<String> paramNames) throws Exception {
        StringBuilder argsStr = new StringBuilder();
        MethodSpec.Builder mb = buildArrange(cleanCall, classNameDot, paramTypes, paramNames, argsStr);

        mb.addCode("\n");
        mb.addComment("Act");
        if (cleanCall.self != null) {
            mb.addStatement("self.$L($L)", mutantCall.methodName, argsStr.toString());
        } else {
            mb.addStatement("$T.$L($L)", ClassName.bestGuess(classNameDot), mutantCall.methodName, argsStr.toString());
        }

        mb.addCode("\n");
        mb.addComment("Skip assertion (only verify non-exceptional execution)");

        return mb.build();
    }

    private java.lang.reflect.Method resolveTargetMethod(String classNameDot, String methodName, java.util.List<LogParser.DeserializedObject> args) {
        try {
            Class<?> targetClass = Class.forName(classNameDot, true, this.sutClassLoader);
            int argCount = args != null ? args.size() : 0;

            Set<java.lang.reflect.Method> allMethods = new HashSet<>();
            allMethods.addAll(java.util.Arrays.asList(targetClass.getMethods()));
            allMethods.addAll(java.util.Arrays.asList(targetClass.getDeclaredMethods()));

            return allMethods.stream()
                    .filter(m -> m.getName().equals(methodName))
                    .filter(m -> m.getParameterCount() == argCount)
                    .filter(m -> isCompatible(m.getParameterTypes(), args))
                    .findFirst()
                    // Fallback: return any method with matching name and arg count if type compatibility fails
                    .orElseGet(() -> allMethods.stream()
                            .filter(m -> m.getName().equals(methodName) && m.getParameterCount() == argCount)
                            .findFirst()
                            .orElse(null));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCompatible(Class<?>[] paramTypes, java.util.List<LogParser.DeserializedObject> args) {
        if (args == null) {
            return paramTypes.length == 0;
        }
        if (paramTypes.length != args.size()) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args.get(i).object;
            if (arg == null) {
                continue;
            }
            Class<?> boxed = boxPrimitive(paramTypes[i]);
            if (!boxed.isAssignableFrom(arg.getClass())) {
                return false;
            }
        }
        return true;
    }

    private Class<?> boxPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        throw new IllegalArgumentException("Unknown primitive type: " + type);
    }

    private List<String> extractParamNames(java.lang.reflect.Method method) {
        java.lang.reflect.Parameter[] params = method.getParameters();
        List<String> names = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (int i = 0; i < params.length; i++) {
            String raw = params[i].isNamePresent() ? params[i].getName() : ("arg" + i);
            String name = sanitizeIdentifier(raw);
            String unique = name;
            int counter = 1;
            while (used.contains(unique)) {
                unique = name + "_" + counter++;
            }
            used.add(unique);
            names.add(unique);
        }
        return names;
    }

    private String sanitizeIdentifier(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "arg";
        }
        String trimmed = raw.trim();
        if (trimmed.equals("this")) {
            return "self";
        }
        String sanitized = trimmed.replaceAll("[^A-Za-z0-9_$]", "_");
        if (!Character.isLetter(sanitized.charAt(0)) && sanitized.charAt(0) != '_' && sanitized.charAt(0) != '$') {
            sanitized = "arg_" + sanitized;
        }
        return sanitized;
    }

    private boolean deserializedEqual(LogParser.DeserializedObject left, LogParser.DeserializedObject right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private String toUpperCamelCase(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "Unknown";
        }
        String sanitized = raw.replaceAll("[^A-Za-z0-9]", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : sanitized.split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
