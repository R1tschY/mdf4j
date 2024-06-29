/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

/**
 * Writer for a long property.
 *
 * @param <T> Type containing property
 */
@FunctionalInterface
public interface WriteLongProperty<T> {

  /**
   * Write long property.
   *
   * @param object Object containing property
   * @param value  New value for property
   */
  void writeLong(T object, long value);
}
