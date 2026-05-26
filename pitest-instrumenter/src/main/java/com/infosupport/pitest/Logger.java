package com.infosupport.pitest;

import org.pitest.reloc.xstream.XStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Logger {
    private static FileOutputStream out;
    private final static XStream xstream = new XStream();

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
            byte[] hash = digest.digest(data.getBytes());
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
        String xml = xstream.toXML(obj);
        String hash = getSha256Hash(xml);
        out.write(("<" + nodeName + " hash=\"" + hash + "\">\n").getBytes());
        out.write(xml.getBytes());
        out.write(("\n</" + nodeName + ">\n").getBytes());
    }

    public static void logCall(String clazz, String method, Object self, Object[] parameters) {
        if (out == null) {
            System.out.println("Logger not initialized, cannot log call to " + clazz + "::" + method);
            return;
        }

        try {
            out.write(("<methodCall class=\"" + escapeXml(clazz) + "\" method=\"" + escapeXml(method) + "\">\n").getBytes());
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
            out.write(("</methodCall>\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logException(Object self, Throwable exception) {
        if (out == null) {
            return;
        }
        try {
            writeObject("except", exception);
            writeObject("selfAfter", self);
            out.write(("</methodCall>\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void startTest(String testName) {
        if (out == null) return;
        try {
            out.write(("<test name=\"" + escapeXml(testName) + "\">\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void endTest() {
        if (out == null) return;
        try {
            out.write("</test>\n".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
