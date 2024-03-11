/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import java.util.function.LongUnaryOperator;

public class LongCell {
  private long value;

  public LongCell(long value) {
    this.value = value;
  }

  public LongCell() {
    // empty
  }

  public LongCell(LongCell clone) {
    this.value = clone.value;
  }

  public long get() {
    return value;
  }

  public void set(long value) {
    this.value = value;
  }

  public long replace(long value) {
    final var res = this.value;
    this.value = value;
    return res;
  }

  public long update(LongUnaryOperator op) {
    this.value = op.applyAsLong(value);
    return this.value;
  }

  public void swap(LongCell other) {
    final var tmp = this.value;
    this.value = other.value;
    other.value = tmp;
  }
}
