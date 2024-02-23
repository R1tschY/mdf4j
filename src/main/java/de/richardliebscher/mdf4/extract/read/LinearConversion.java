/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.ChannelConversionBlock;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;

public class LinearConversion implements ValueRead {

  private final double p1;
  private final double p2;
  private final ValueRead inner;

  public LinearConversion(ChannelConversionBlock cc, ValueRead valueRead) {
    this.p1 = Double.longBitsToDouble(cc.getVals()[0]);
    this.p2 = Double.longBitsToDouble(cc.getVals()[1]);
    this.inner = valueRead;
  }

  @Override
  public <T, P> T read(RecordBuffer input, Visitor<T, P> visitor, P param) throws IOException {
    // TODO: add shortcut for p1 == 0.0 and p2 == 1.0
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
        return visitor.visitF64(value * p2 + p1, param);
      }

      @Override
      public T visitInvalid(P param) throws IOException {
        return visitor.visitInvalid(param);
      }
    }, param);
  }
}
