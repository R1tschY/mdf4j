/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

/**
 * Writer for a boolean property.
 *
 * @param <T> Type containing property
 */
@FunctionalInterface
public interface WriteBooleanProperty<T> {

  /**
   * Write boolean property.
   *
   * @param object Object containing property
   * @param value  New value for property
   */
  void writeBoolean(T object, boolean value);
}
