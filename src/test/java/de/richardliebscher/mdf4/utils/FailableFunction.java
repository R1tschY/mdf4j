package de.richardliebscher.mdf4.utils;

@FunctionalInterface
public interface FailableFunction<T, R, E extends Throwable> {
  R apply(T t) throws E;
}
