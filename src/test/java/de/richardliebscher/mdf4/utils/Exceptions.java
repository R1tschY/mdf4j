package de.richardliebscher.mdf4.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

public class Exceptions {
  public static <T, R> Function<T, R> wrapIOException(FailableFunction<T, R, IOException> fn) {
    return x -> {
      try {
        return fn.apply(x);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    };
  }
}
