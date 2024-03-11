/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet.de;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.parquet.io.api.RecordConsumer;

@RequiredArgsConstructor
@Getter
final class Int64FieldDeserialize implements DeserializeInto<RecordConsumer>,
    de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

  @Override
  public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
      throws IOException {
    deserializer.deserialize_value(this, recordConsumer);
  }

  @Override
  public String expecting() {
    return "64-bit integer";
  }

  @Override
  public Void visitU32(int value, RecordConsumer recordConsumer) {
    return visitI64(UnsignedInteger.toLong(value), recordConsumer);
  }

  @Override
  public Void visitU64(long value, RecordConsumer recordConsumer) {
    if (value < 0) {
      throw new ArithmeticException("long overflow"); // TODO
    }
    return visitI64(value, recordConsumer);
  }

  @Override
  public Void visitI64(long value, RecordConsumer recordConsumer) {
    recordConsumer.addLong(value);
    return null;
  }

  @Override
  public Void visitInvalid(RecordConsumer recordConsumer) {
    return null;
  }
}
