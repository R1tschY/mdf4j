/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

/**
 * Writer for a float property.
 *
 * @param <T> Type containing property
 */
@FunctionalInterface
public interface WriteFloatProperty<T> {

  /**
   * Write float property.
   *
   * @param object Object containing property
   * @param value  New value for property
   */
  void writeFloat(T object, float value);
}
