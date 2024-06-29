/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

/**
 * Writer for a int property.
 *
 * @param <T> Type containing property
 */
@FunctionalInterface
public interface WriteIntProperty<T> {

  /**
   * Write int property.
   *
   * @param object Object containing property
   * @param value  New value for property
   */
  void writeInt(T object, int value);
}
