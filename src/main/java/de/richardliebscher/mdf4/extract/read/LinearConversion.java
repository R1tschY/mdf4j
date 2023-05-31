/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.ChannelConversion;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.Visitor;
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
      public String expecting() {
        return "numeric value";
      }

      @Override
      public T visitU64(long value) throws IOException {
        return visitF64(UnsignedLong.toDoubleValue(value));
      }

      @Override
      public T visitI64(long value) throws IOException {
        return visitF64(value);
      }

      @Override
      public T visitF64(double value) throws IOException {
        return visitor.visitF64(value * p2 + p1);
      }

      @Override
      public T visitInvalid() throws IOException {
        return visitor.visitInvalid();
      }
    });
  }
}
