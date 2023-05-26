/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Hold value or exception.
 *
 * <p>Used in interfaces that do not allow to throw checked exceptions. Instead, the exception is
 * caught and using this object.
 *
 * @param <T> Success type
 * @param <E> Exception/Error type
 */
public abstract class Result<T, E extends Throwable> {

  // sealed class pattern
  private Result() {
    super();
  }

  /**
   * Try throwing operation and catch exception in result object.
   *
   * @param f   Throwing operation
   * @param <T> Success type
   * @param <E> Exception/Error type
   * @return Result
   */
  @SuppressWarnings("unchecked")
  public static <T, E extends Throwable> Result<T, E> try_(ThrowingSupplier<T, E> f) {
    try {
      return new Ok<>(f.get());
    } catch (RuntimeException exp) {
      throw exp;
    } catch (Throwable exp) {
      return new Err<>((E) exp);
    }
  }

  /**
   * Try throwing procedure and catch exception in result object.
   *
   * @param f   Throwing procedure
   * @param <E> Exception/Error type
   * @return Result
   */
  @SuppressWarnings("unchecked")
  public static <E extends Throwable> Result<Void, E> try_(ThrowingCallable<E> f) {
    try {
      f.call();
      return new Ok<>(null);
    } catch (RuntimeException exp) {
      throw exp;
    } catch (Throwable exp) {
      return new Err<>((E) exp);
    }
  }

  /**
   * Try throwing I/O operation and catch exception in result object.
   *
   * @param f   Throwing I/O operation
   * @param <T> Success type
   * @return Result
   */
  public static <T> Result<T, IOException> tryIo(ThrowingSupplier<T, IOException> f) {
    try {
      return new Ok<>(f.get());
    } catch (IOException exp) {
      return new Err<>(exp);
    }
  }

  /**
   * Try throwing I/O procedure and catch exception in result object.
   *
   * @param f Throwing procedure
   * @return Result
   */
  public static Result<Void, IOException> tryIo(ThrowingCallable<IOException> f) {
    try {
      f.call();
      return new Ok<>(null);
    } catch (IOException exp) {
      return new Err<>(exp);
    }
  }

  /**
   * Return whether object holds a value.
   *
   * @return {@code true} iff object holds a value
   */
  public abstract boolean isOk();

  /**
   * Return whether object holds an error.
   *
   * @return {@code true} iff object holds an error
   */
  public abstract boolean isErr();

  /**
   * Get value or throw containing exception.
   *
   * @return value, if it exists
   * @throws E Throws exception iff object contains exception
   */
  public abstract T get() throws E;

  /**
   * Unwrap result object.
   *
   * @return Value
   * @throws RuntimeException If object contains exception. {@link RuntimeException} wraps checked
   *                          exception.
   */
  public abstract T unwrap();

  /**
   * Get value or return a default value.
   *
   * @param defaultValue Default value when no value exists
   * @return Value or default value, iff no value exists
   */
  public abstract T getOr(T defaultValue);

  /**
   * Get value or use return value of supplier function.
   *
   * @param f Supplier function
   * @return Value or return value of supplier function, iff no value exists
   */
  public abstract T getOrElse(Supplier<T> f);

  /**
   * Transform value.
   *
   * @param f   Non-failable transform function
   * @param <U> New success type
   * @return Result with transformed value or original exception.
   */
  public abstract <U> Result<U, E> map(Function<T, U> f);

  /**
   * Transform exception.
   *
   * @param f   Non-failable exception transform function
   * @param <F> New exception type
   * @return Result with transformed exception value or original value.
   */
  public abstract <F extends Throwable> Result<T, F> mapErr(Function<E, F> f);

  /**
   * Success state of result object.
   *
   * @param <T> Success type
   * @param <E> Exception/Error type
   */
  public static final class Ok<T, E extends Throwable> extends Result<T, E> {

    private final T value;

    public Ok(T value) {
      this.value = value;
    }

    @Override
    public boolean isOk() {
      return true;
    }

    @Override
    public boolean isErr() {
      return false;
    }

    @Override
    public T get() throws E {
      return value;
    }

    @Override
    public T unwrap() {
      return value;
    }

    @Override
    public T getOr(T defaultValue) {
      return value;
    }

    @Override
    public T getOrElse(Supplier<T> f) {
      return value;
    }

    @Override
    public <U> Result<U, E> map(Function<T, U> f) {
      return new Result.Ok<>(f.apply(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F extends Throwable> Result<T, F> mapErr(Function<E, F> f) {
      return (Result<T, F>) this;
    }

    @Override
    public String toString() {
      return "Ok{" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      return Objects.equals(value, ((Ok<?, ?>) o).value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }

  /**
   * Error state of result object.
   *
   * @param <T> Success type
   * @param <E> Exception/Error type
   */
  public static final class Err<T, E extends Throwable> extends Result<T, E> {

    private final E value;

    /**
     * Construct error result.
     *
     * @param value Non-null Exception
     */
    public Err(E value) {
      this.value = requireNonNull(value);
    }

    @Override
    public boolean isOk() {
      return false;
    }

    @Override
    public boolean isErr() {
      return true;
    }

    @Override
    public T get() throws E {
      throw value;
    }

    @Override
    public T unwrap() {
      throw new RuntimeException(value);
    }

    @Override
    public T getOr(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T getOrElse(Supplier<T> f) {
      return f.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Result<U, E> map(Function<T, U> f) {
      return (Result<U, E>) this;
    }

    @Override
    public <F extends Throwable> Result<T, F> mapErr(Function<E, F> f) {
      return new Result.Err<>(f.apply(value));
    }

    @Override
    public String toString() {
      return "Err{" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      return Objects.equals(value, ((Err<?, ?>) o).value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }

  /**
   * Failable function.
   *
   * @param <T> Return value type
   * @param <E> Exception type
   */
  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Throwable> {

    /**
     * Call function.
     *
     * @return Return value
     * @throws E Any exception
     */
    T get() throws E;
  }

  /**
   * Failable procedure.
   *
   * @param <E> Exception type
   */
  @FunctionalInterface
  public interface ThrowingCallable<E extends Throwable> {

    /**
     * Call procedure.
     *
     * @throws E Any exception
     */
    void call() throws E;
  }
}
