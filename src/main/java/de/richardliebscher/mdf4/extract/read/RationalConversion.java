/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;

public class RationalConversion implements ValueRead {

  private final double p1;
  private final double p2;
  private final double p3;
  private final double p4;
  private final double p5;
  private final double p6;
  private final ValueRead inner;

  public RationalConversion(long[] vals, ValueRead valueRead) {
    this.p1 = Double.longBitsToDouble(vals[0]);
    this.p2 = Double.longBitsToDouble(vals[1]);
    this.p3 = Double.longBitsToDouble(vals[2]);
    this.p4 = Double.longBitsToDouble(vals[3]);
    this.p5 = Double.longBitsToDouble(vals[4]);
    this.p6 = Double.longBitsToDouble(vals[5]);
    this.inner = valueRead;
  }

  @Override
  public <T, P> T read(RecordBuffer input, Visitor<T, P> visitor, P param) throws IOException {
    return inner.read(input, new Visitor<>() {
      @Override
      public String expecting() {
        return "numeric value";
      }

      @Override
      public T visitU64(long value, P param) throws IOException {
        return visitF64(UnsignedLong.toDoubleValue(value), param);
      }

      @Override
      public T visitI64(long value, P param) throws IOException {
        return visitF64(value, param);
      }

      @Override
      public T visitF64(double value, P param) throws IOException {
        return visitor.visitF64((p1 * value * value + p2 * value + p3)
            / (p4 * value * value + p5 * value + p6), param);
      }

      @Override
      public T visitInvalid(P param) throws IOException {
        return visitor.visitInvalid(param);
      }
    }, param);
  }
}
