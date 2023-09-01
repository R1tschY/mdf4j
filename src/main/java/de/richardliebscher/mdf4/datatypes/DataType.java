/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.datatypes;

/**
 * Data type of channel values.
 */
public interface DataType {

  /**
   * Accept visitor.
   *
   * @param visitor Visitor
   * @param <R>     Return type of visitor
   * @param <E>     Exception type of visitor
   * @return Return value of visitor
   * @throws E Exception thrown from visitor
   */
  <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E;

  /**
   * Visitor.
   *
   * @param <R> Return type
   * @param <E> Exception type
   */
  interface Visitor<R, E extends Throwable> {

    /**
     * Visit {@link IntegerType}.
     *
     * @param type {@link IntegerType}
     * @return Any value
     * @throws E Any exception
     */
    default R visit(IntegerType type) throws E {
      return visitElse(type);
    }

    /**
     * Visit {@link UnsignedIntegerType}.
     *
     * @param type {@link UnsignedIntegerType}
     * @return Any value
     * @throws E Any exception
     */
    default R visit(UnsignedIntegerType type) throws E {
      return visitElse(type);
    }

    /**
     * Visit {@link FloatType}.
     *
     * @param type {@link FloatType}
     * @return Any value
     * @throws E Any exception
     */
    default R visit(FloatType type) throws E {
      return visitElse(type);
    }

    /**
     * Visit {@link StringType}.
     *
     * @param type {@link StringType}
     * @return Any value
     * @throws E Any exception
     */
    default R visit(StringType type) throws E {
      return visitElse(type);
    }

    /**
     * Visit unhandled type.
     *
     * @param type Data type
     * @return Any value
     * @throws E Any exception
     */
    R visitElse(DataType type) throws E;
  }
}
