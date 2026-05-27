package com.infosupport.pitest;

import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.MutationDetails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MutantHash {

    private MutantHash() {
    }

    public static String hash(Mutant mutant) {
        return hash(mutant.getDetails());
    }

    public static String hash(MutationDetails details) {
        return hashIdString(idString(details));
    }

    public static String idString(MutationDetails details) {
        String clazz = details.getClassName().asJavaName();
        String method = details.getMethod();
        String methodDescription = details.getId().getLocation().getMethodDesc();
        int lineNumber = details.getLineNumber();
        String mutator = details.getMutator();
        java.util.List<Integer> indexes = details.getId().getIndexes();

        return clazz + "-" + method + "-" + methodDescription + "-" + lineNumber + "-" + mutator + "-" + indexes;
    }

    private static String hashIdString(String idStr) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(idStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(Math.abs(idStr.hashCode()));
        }
    }
}

