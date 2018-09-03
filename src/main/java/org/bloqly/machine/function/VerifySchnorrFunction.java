package org.bloqly.machine.function;

@FunctionalInterface
public interface VerifySchnorrFunction {

    Boolean apply(String message, String signature, String publicKey);
}
