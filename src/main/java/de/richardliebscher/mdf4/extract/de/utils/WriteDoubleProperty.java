/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

/**
 * Writer for a double property.
 *
 * @param <T> Type containing property
 */
@FunctionalInterface
public interface WriteDoubleProperty<T> {

  /**
   * Write double property.
   *
   * @param object Object containing property
   * @param value  New value for property
   */
  void writeDouble(T object, double value);
}
