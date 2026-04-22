package com.infosupport.pitest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Logger {
    private static FileOutputStream out;

    public static void setOutput(final FileOutputStream out) {
        Logger.out = out;
    }

    public static void logCall(String clazz, String method, Object[] parameters) {
        Thread.dumpStack();

        try {
            out.write((clazz + "::" + method).getBytes());
            out.write("\n".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
