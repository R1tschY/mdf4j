/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public class CustomFlags {

  protected final int value;

  public static CustomFlags of(int flags) {
    return new CustomFlags(flags);
  }

  private CustomFlags(int value) {
    this.value = value;
  }

  public CustomFlags merge(CustomFlags other) {
    return of(this.value | other.value);
  }

  public boolean test(CustomFlags test) {
    return (value & test.value) == test.value;
  }

  public boolean testBit(int bitNumber) {
    final var toTest = 1 << bitNumber;
    return (value & toTest) != 0;
  }

  public boolean anyOf(CustomFlags test) {
    return (value & test.value) != 0;
  }

  public boolean isEmpty() {
    return value == 0;
  }
}
