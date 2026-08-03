package com.infosupport.pitest.carve;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class LogParser {
    private final XStream xstream;
    private final Map<String, DeserializedObject> objectCache = new LinkedHashMap<>();


    public LogParser(XStream xstream) {
        this.xstream = xstream;
    }

    public static class TestRun {
        public String name;
        public java.util.Map<String, java.util.Map<String, java.util.List<MethodCall>>> calls = new java.util.LinkedHashMap<>();
    }

    public static class DeserializedObject {
        public Object object;
        public String hash;

        public DeserializedObject(Object object, String hash) {
            this.object = object;
            this.hash = hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeserializedObject that = (DeserializedObject) o;
            if (this.hash != null && that.hash != null && this.hash.equals(that.hash)) return true;
            return java.util.Objects.equals(this.object, that.object);
        }

        public boolean distinguishable(DeserializedObject other) {
            if (this.hash.equals(other.hash)) return false;
            return !java.util.Objects.equals(this.object, other.object);
        }

        @Override
        public String toString() {
            return object + " (hash: " + hash + ")";
        }
    }

    public static class MethodCall {
        public String className;
        public String methodName;
        public DeserializedObject self;
        public java.util.List<DeserializedObject> args = new java.util.ArrayList<>();
        public DeserializedObject returnValue;
        public DeserializedObject exception;
        public DeserializedObject selfAfter;

        public boolean matchesInput(MethodCall other) {
            if (!java.util.Objects.equals(className, other.className)) return false;
            if (!java.util.Objects.equals(methodName, other.methodName)) return false;
            if (!java.util.Objects.equals(self, other.self)) return false;
            if (args.size() != other.args.size()) return false;
            for (int i = 0; i < args.size(); i++) {
                if (!java.util.Objects.equals(args.get(i), other.args.get(i))) return false;
            }
            return true;
        }

        public boolean distinguishable(MethodCall other) {
            // Checks if the outputs of the calls are distinguishable, assuming they have the same inputs
            if (!matchesInput(other)) {
                throw new IllegalArgumentException("Cannot compare distinguishability of calls with different inputs");
            }
            if (selfAfter != null && other.selfAfter != null) {
                // note: selfAfter will never be null for only one; this depends on method staticity which does not get mutated
                if (!selfAfter.equals(other.selfAfter)) {
                    return true; // The state after the call is different, so they are distinguishable
                }
            }
            if (exception != null && other.exception != null) {
                return this.exception.distinguishable(other.exception);
            } else if (exception == null && other.exception == null) {
                return this.returnValue.distinguishable(other.returnValue);
            } else {
                // One call threw an exception while the other did not, so they are distinguishable
                return true;
            }
        }
    }

    public Map<String, TestRun> parseLogFile(File xmlFile) {
        return parseLogFiles(java.util.Collections.singletonList(xmlFile));
    }

    public Map<String, TestRun> parseLogFiles(Collection<File> xmlFiles) {
        Map<String, TestRun> testRuns = new LinkedHashMap<>();

        InputStream combinedStream = new SequenceInputStream(
                new ByteArrayInputStream("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n<roots>\n".getBytes()),
                new SequenceInputStream(
                        xmlFiles.stream()
                                .filter(File::exists)
                                .map(file -> {
                                    try {
                                        FileInputStream fileIn = new FileInputStream(file);
                                        GZIPInputStream gzIn = new GZIPInputStream(fileIn);
                                        return (InputStream)gzIn;
                                    } catch (IOException e) {
                                        throw new RuntimeException("Error opening file: " + file, e);
                                    }
                                })
                                .reduce(SequenceInputStream::new)
                                .orElse(new ByteArrayInputStream(new byte[0])),
                        new ByteArrayInputStream("</roots>".getBytes())
                )
        );

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(combinedStream);

            TestRun currentTest = null;
            java.util.Stack<MethodCall> callStack = new java.util.Stack<>();

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String nodeName = reader.getLocalName();
                    if ("test".equals(nodeName)) {
                        currentTest = new TestRun();
                        currentTest.name = reader.getAttributeValue(null, "name");
                        testRuns.put(currentTest.name, currentTest);
                    } else if ("methodCall".equals(nodeName)) {
                        MethodCall call = new MethodCall();
                        call.className = reader.getAttributeValue(null, "class");
                        call.methodName = reader.getAttributeValue(null, "method");
                        if (currentTest != null) {
                            var classCalls = currentTest.calls.computeIfAbsent(call.className, k -> new LinkedHashMap<>());
                            var methodCalls = classCalls.computeIfAbsent(call.methodName, k -> new java.util.ArrayList<>());
                            methodCalls.add(call);
                        }
                        callStack.push(call);
                    } else if ("self".equals(nodeName)) {
                        String hash = reader.getAttributeValue(null, "hash");
                        StaxReader staxReader = new StaxReader(new QNameMap(), reader);
                        Object deserializedObject = xstream.unmarshal(staxReader);
                        callStack.peek().self = this.objectCache.computeIfAbsent(hash, h -> new DeserializedObject(deserializedObject, h));
                    } else if ("arg".equals(nodeName)) {
                        String hash = reader.getAttributeValue(null, "hash");
                        StaxReader staxReader = new StaxReader(new QNameMap(), reader);
                        Object deserializedObject = xstream.unmarshal(staxReader);
                        callStack.peek().args.add(this.objectCache.computeIfAbsent(hash, h -> new DeserializedObject(deserializedObject, h)));
                    } else if ("return".equals(nodeName)) {
                        String hash = reader.getAttributeValue(null, "hash");
                        StaxReader staxReader = new StaxReader(new QNameMap(), reader);
                        Object deserializedObject = xstream.unmarshal(staxReader);
                        callStack.peek().returnValue = this.objectCache.computeIfAbsent(hash, h -> new DeserializedObject(deserializedObject, h));
                    } else if ("except".equals(nodeName)) {
                        String hash = reader.getAttributeValue(null, "hash");
                        StaxReader staxReader = new StaxReader(new QNameMap(), reader);
                        Object deserializedObject = xstream.unmarshal(staxReader);
                        callStack.peek().exception = this.objectCache.computeIfAbsent(hash, h -> new DeserializedObject(deserializedObject, h));
                    } else if ("selfAfter".equals(nodeName)) {
                        String hash = reader.getAttributeValue(null, "hash");
                        StaxReader staxReader = new StaxReader(new QNameMap(), reader);
                        Object deserializedObject = xstream.unmarshal(staxReader);
                        callStack.peek().selfAfter = this.objectCache.computeIfAbsent(hash, h -> new DeserializedObject(deserializedObject, h));
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("methodCall".equals(reader.getLocalName())) {
                        if (!callStack.isEmpty()) callStack.pop();
                    } else if ("test".equals(reader.getLocalName())) {
                        currentTest = null;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing log file(s): " + xmlFiles, e);
        }
        return testRuns;
    }
}
