/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

/**
 * Writer for a boxed property.
 *
 * @param <T> Type containing property
 * @param <V> Type of property
 */
@FunctionalInterface
public interface WriteProperty<T, V> {

  /**
   * Write property.
   *
   * @param object Object containing property
   * @param value  New value for property
   */
  void write(T object, V value);
}
