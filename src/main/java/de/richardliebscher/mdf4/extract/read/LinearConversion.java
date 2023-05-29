/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.ChannelConversion;
import de.richardliebscher.mdf4.extract.de.Half;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.internal.Unsigned;
import java.io.IOException;

public class LinearConversion implements ValueRead {

  private final double p1;
  private final double p2;
  private final ValueRead inner;

  public LinearConversion(ChannelConversion cc, ValueRead valueRead) {
    this.p1 = Double.longBitsToDouble(cc.getVals()[0]);
    this.p2 = Double.longBitsToDouble(cc.getVals()[1]);
    this.inner = valueRead;
  }

  @Override
  public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
    // TODO: add shortcut for p1 == 0.0 and p2 == 1.0
    return inner.read(input, new Visitor<>() {
      @Override
      public T visitU8(byte value) {
        return visitor.visitF64(Byte.toUnsignedInt(value) * p2 + p1);
      }

      @Override
      public T visitU16(short value) {
        return visitor.visitF64(Short.toUnsignedInt(value) * p2 + p1);
      }

      @Override
      public T visitU32(int value) {
        return visitor.visitF64(Integer.toUnsignedLong(value) * p2 + p1);
      }

      @Override
      public T visitU64(long value) {
        return visitor.visitF64(Unsigned.doubleValue(value) * p2 + p1);
      }

      @Override
      public T visitI8(byte value) {
        return visitor.visitF64(value * p2 + p1);
      }

      @Override
      public T visitI16(short value) {
        return visitor.visitF64(value * p2 + p1);
      }

      @Override
      public T visitI32(int value) {
        return visitor.visitF64(value * p2 + p1);
      }

      @Override
      public T visitI64(long value) {
        return visitor.visitF64(value * p2 + p1);
      }

      @Override
      public T visitF16(short value) {
        return visitor.visitF64(Half.shortBitsToFloat(value) * p2 + p1);
      }

      @Override
      public T visitF32(float value) {
        return visitor.visitF64(value * p2 + p1);
      }

      @Override
      public T visitF64(double value) {
        return visitor.visitF64(value * p2 + p1);
      }

      @Override
      public T visitInvalid() {
        return visitor.visitInvalid();
      }
    });
  }
}
