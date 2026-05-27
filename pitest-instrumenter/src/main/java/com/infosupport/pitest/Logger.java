package com.infosupport.pitest;

import org.pitest.reloc.xstream.XStream;
import org.pitest.reloc.xstream.io.HierarchicalStreamWriter;
import org.pitest.reloc.xstream.io.xml.PrettyPrintWriter;
import org.pitest.reloc.xstream.io.xml.XppDriver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Xml11Driver extends XppDriver {
    @Override
    public HierarchicalStreamWriter createWriter(Writer out) {
        return new PrettyPrintWriter(out, PrettyPrintWriter.XML_1_1_REPLACEMENT);
    }
}

public class Logger {
    private static FileOutputStream out;
    private final static XStream xstream = new XStream(new Xml11Driver());


    public static void setOutput(final FileOutputStream out) {
        if (Logger.out != null) {
            try {
                Logger.out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Logger.out = out;
    }

    private static String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    private static String getSha256Hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeObject(String nodeName, Object obj) throws IOException {
        String xml;
        String hash;
        try {
            xml = xstream.toXML(obj);
        } catch (Exception e) {
            xml = xstream.toXML(null);
        }
        hash = getSha256Hash(xml);
        out.write(("<" + nodeName + " hash=\"" + hash + "\">\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(("\n</" + nodeName + ">\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));

    }

    public static void logCall(String clazz, String method, Object self, Object[] parameters) {
        if (out == null) {
            System.out.println("Logger not initialized, cannot log call to " + clazz + "::" + method);
            return;
        }

        try {
            out.write(("<methodCall class=\"" + escapeXml(clazz) + "\" method=\"" + escapeXml(method) + "\">\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            writeObject("self", self);
            for (Object parameter : parameters) {
                writeObject("arg", parameter);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logReturn(Object self, Object returnValue) {
        if (out == null) {
            return;
        }
        try {
            writeObject("return", returnValue);
            writeObject("selfAfter", self);
            out.write(("</methodCall>\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logException(Object self, Throwable exception) {
        if (out == null) {
            return;
        }
        System.out.println("Exception in method call: " + exception);
        try {
            writeObject("except", exception);
            writeObject("selfAfter", self);
            out.write(("</methodCall>\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void startTest(String testName) {
        if (out == null) return;
        try {
            out.write(("<test name=\"" + escapeXml(testName) + "\">\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void endTest() {
        if (out == null) return;
        try {
            out.write("</test>\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
