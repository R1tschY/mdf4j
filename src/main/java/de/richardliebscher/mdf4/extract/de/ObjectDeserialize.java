/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import de.richardliebscher.mdf4.internal.Unsigned;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class ObjectDeserialize implements Deserialize<Object> {

  @Override
  public Object deserialize(Deserializer deserializer) throws IOException {
    return deserializer.deserialize_value(new Visitor<>() {
      @Override
      public Object visitU8(byte value) {
        return new UnsignedByte(value);
      }

      @Override
      public Object visitU16(short value) {
        return new UnsignedShort(value);
      }

      @Override
      public Object visitU32(int value) {
        return new UnsignedInteger(value);
      }

      @Override
      public Object visitU64(long value) {
        return new UnsignedLong(value);
      }

      @Override
      public Object visitI8(byte value) {
        return value;
      }

      @Override
      public Object visitI16(short value) {
        return value;
      }

      @Override
      public Object visitI32(int value) {
        return value;
      }

      @Override
      public Object visitI64(long value) {
        return value;
      }

      @Override
      public Object visitF32(float value) {
        return value;
      }

      @Override
      public Object visitF64(double value) {
        return value;
      }

      @Override
      public Object visitInvalid() {
        return Invalid.get();
      }
    });
  }

  @RequiredArgsConstructor
  @Getter
  public static final class UnsignedByte extends Number {

    private final byte value;

    @Override
    public int intValue() {
      return Byte.toUnsignedInt(value);
    }

    @Override
    public long longValue() {
      return intValue();
    }

    @Override
    public float floatValue() {
      return intValue();
    }

    @Override
    public double doubleValue() {
      return intValue();
    }

    @Override
    public String toString() {
      return String.valueOf(intValue());
    }

    public boolean equals(final Object o) {
      if (o instanceof UnsignedByte) {
        return this.value == ((UnsignedByte) o).value;
      } else {
        return false;
      }
    }

    public int hashCode() {
      return Byte.hashCode(this.value);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static final class UnsignedShort extends Number {

    private final short value;

    @Override
    public int intValue() {
      return Short.toUnsignedInt(value);
    }

    @Override
    public long longValue() {
      return intValue();
    }

    @Override
    public float floatValue() {
      return intValue();
    }

    @Override
    public double doubleValue() {
      return intValue();
    }

    @Override
    public String toString() {
      return String.valueOf(intValue());
    }

    public boolean equals(final Object o) {
      if (o instanceof UnsignedShort) {
        return this.value == ((UnsignedShort) o).value;
      } else {
        return false;
      }
    }

    public int hashCode() {
      return Short.hashCode(this.value);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class UnsignedInteger extends Number {

    private final int value;

    @Override
    public int intValue() {
      return (int) longValue();
    }

    @Override
    public long longValue() {
      return Integer.toUnsignedLong(value);
    }

    @Override
    public float floatValue() {
      return longValue();
    }

    @Override
    public double doubleValue() {
      return longValue();
    }

    @Override
    public String toString() {
      return String.valueOf(longValue());
    }

    public boolean equals(final Object o) {
      if (o instanceof UnsignedInteger) {
        return this.value == ((UnsignedInteger) o).value;
      } else {
        return false;
      }
    }

    public int hashCode() {
      return Integer.hashCode(this.value);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class UnsignedLong extends Number {

    private final long value;

    @Override
    public int intValue() {
      return (int) longValue();
    }

    @Override
    public long longValue() {
      return Unsigned.longValue(value);
    }

    @Override
    public float floatValue() {
      return Unsigned.floatValue(value);
    }

    @Override
    public double doubleValue() {
      return Unsigned.doubleValue(value);
    }

    @Override
    public String toString() {
      return Long.toUnsignedString(value);
    }

    public boolean equals(final Object o) {
      if (o instanceof UnsignedLong) {
        return this.value == ((UnsignedLong) o).value;
      } else {
        return false;
      }
    }

    public int hashCode() {
      return Long.hashCode(this.value);
    }
  }
}
