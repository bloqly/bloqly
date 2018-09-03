package org.bloqly.machine.function;

@FunctionalInterface
public interface VerifyFunction {

    Boolean apply(String message, String signature, String publicKey);
}
