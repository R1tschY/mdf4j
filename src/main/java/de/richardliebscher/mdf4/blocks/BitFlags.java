/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import java.util.Arrays;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class BitFlags<T extends Enum<T> & BitFlag> {
  private final int value;
  private final Class<T> cls;

  public static <T extends Enum<T> & BitFlag> BitFlags<T> of(int value, Class<T> cls) {
    return new BitFlags<>(value, cls);
  }

  @SafeVarargs
  public static <T extends Enum<T> & BitFlag> BitFlags<T> of(Class<T> cls, T... values) {
    return new BitFlags<>(Arrays.stream(values)
        .mapToInt(x -> 1 << x.bitNumber())
        .reduce(0, (x, y) -> x | y), cls);
  }

  private BitFlags(int value, Class<T> cls) {
    this.value = value;
    this.cls = cls;
  }

  public BitFlags<T> merge(BitFlags<T> other) {
    return new BitFlags<>(this.value | other.value, cls);
  }

  public boolean allOf(BitFlags<T> test) {
    return (value & test.value) == test.value;
  }

  public boolean isSet(T bit) {
    return (value & 1 << bit.bitNumber()) != 0;
  }

  public boolean isSet(int bit) {
    return (value & 1 << bit) != 0;
  }

  public boolean anyOf(BitFlags<T> test) {
    return (value & test.value) != 0;
  }

  public boolean isEmpty() {
    return value == 0;
  }

  private BitFlags<T> allSet() {
    int value = 0;
    for (var flag : cls.getEnumConstants()) {
      value |= flag.bitNumber();
    }
    return of(value, cls);
  }

  public boolean hasUnknown() {
    return ((~allSet().value) & value) != 0;
  }

  @Override
  public String toString() {
    var v = value;
    final var sb = new StringBuilder();
    for (var flag : cls.getEnumConstants()) {
      if (isSet(flag)) {
        if (sb.length() != 0) {
          sb.append(",");
        }
        sb.append(flag.name());
        v &= ~(1 << flag.bitNumber());
      }
    }
    if (v != 0) {
      for (int i = 0; i < 32; i++) {
        if ((v & (1 << i)) != 0) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("UnknownBit").append(i);
        }
      }
    }
    return sb.toString();
  }
}
