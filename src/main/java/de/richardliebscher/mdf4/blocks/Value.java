/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Value<T extends Enum<T> & KnownValue> {
  private final int intValue;
  private final T knownValue;
  private final Class<T> type;

  Value(byte value, Class<T> type) {
    final var intValue = UnsignedByte.toInt(value);
    this.intValue = intValue;
    this.type = type;
    this.knownValue = Arrays.stream(type.getEnumConstants())
        .filter(x -> x.intValue() == intValue)
        .findFirst()
        .orElse(null);
  }

  public static <T extends Enum<T> & KnownValue> Value<T> empty(Class<T> type) {
    return new Value<>((byte) 0, type);
  }

  public static <T extends Enum<T> & KnownValue> Value<T> of(byte intValue, Class<T> type) {
    return new Value<>(intValue, type);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Enum<T> & KnownValue> Value<T> of(T value) {
    return new Value<>(value.intValue(), value, (Class<T>) value.getClass());
  }

  public int asInt() {
    return intValue;
  }

  public short asShort() {
    return (short) intValue;
  }

  public byte asByte() {
    return (byte) intValue;
  }

  public boolean isKnown() {
    return knownValue != null;
  }

  @Override
  public String toString() {
    final var name = isKnown() ? knownValue.name() : "<UNKNOWN>";
    return intValue + " (" + name + ")";
  }
}
