package com.infosupport.pitest;

import org.pitest.reloc.xstream.XStream;

import java.io.FileOutputStream;
import java.io.IOException;

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

    public static void logCall(String clazz, String method, Object[] parameters) {
        if (out == null) {
            System.out.println("Logger not initialized, cannot log call to " + clazz + "::" + method);
            return;
        }

        try {
            out.write(("<methodCall class=\"" + clazz + "\" method=\"" + method + "\">\n").getBytes());
            out.write(("<args>\n").getBytes());
            for (Object parameter : parameters) {
                xstream.toXML(parameter, out);
                out.write("\n".getBytes());
            }
            out.write(("</args>\n").getBytes());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logReturn(String clazz, String method, Object returnValue) {
        if (out == null) {
            return;
        }
        try {
            out.write("<return>\n".getBytes());
            xstream.toXML(returnValue, out);
            out.write(("\n</return>\n</methodCall>\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logException(String clazz, String method, Throwable exception) {
        if (out == null) {
            return;
        }
        try {
            out.write("<except>\n".getBytes());
            xstream.toXML(exception, out);
            out.write(("\n</except>\n</methodCall>\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
