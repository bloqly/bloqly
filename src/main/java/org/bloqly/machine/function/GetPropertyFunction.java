package org.bloqly.machine.function;

@FunctionalInterface
public interface GetPropertyFunction {

    Object apply(String target, String key, Object defaultValue);
}