/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import java.util.function.IntUnaryOperator;

public class IntCell {
  private int value;

  public IntCell(int value) {
    this.value = value;
  }

  public IntCell() {
    // empty
  }

  public IntCell(IntCell clone) {
    this.value = clone.value;
  }

  public int get() {
    return value;
  }

  public void set(int value) {
    this.value = value;
  }

  public int replace(int value) {
    final var res = this.value;
    this.value = value;
    return res;
  }

  public int update(IntUnaryOperator op) {
    this.value = op.applyAsInt(value);
    return this.value;
  }

  public void swap(IntCell other) {
    final var tmp = this.value;
    this.value = other.value;
    other.value = tmp;
  }
}
