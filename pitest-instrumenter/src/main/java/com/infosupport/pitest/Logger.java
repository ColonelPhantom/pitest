package com.infosupport.pitest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Logger {
    private static FileOutputStream out;

    public static void setOutput(final FileOutputStream out) {
        Logger.out = out;
    }

    public static void logCall(String clazz, String method, Object[] parameters) {
        if (out == null) {
            System.out.println("Logger not initialized, cannot log call to " + clazz + "::" + method);
            return;
        }

        try {
            out.write((clazz + "::" + method + " args: " + Arrays.deepToString(parameters)).getBytes());
            out.write("\n".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logReturn(String clazz, String method, Object returnValue) {
        if (out == null) {
            return;
        }
        try {
            out.write((clazz + "::" + method + " return: " + (returnValue != null ? returnValue.toString() : "null")).getBytes());
            out.write("\n".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logException(String clazz, String method, Throwable exception) {
        if (out == null) {
            return;
        }
        try {
            out.write((clazz + "::" + method + " exception: " + (exception != null ? exception.toString() : "null")).getBytes());
            out.write("\n".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
