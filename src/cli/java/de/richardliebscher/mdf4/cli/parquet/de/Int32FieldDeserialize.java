/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet.de;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedShort;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.parquet.io.api.RecordConsumer;

@RequiredArgsConstructor
@Getter
final class Int32FieldDeserialize implements DeserializeInto<RecordConsumer>,
    de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

  @Override
  public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
      throws IOException {
    deserializer.deserialize_value(this, recordConsumer);
  }

  @Override
  public String expecting() {
    return "32-bit integer";
  }

  @Override
  public Void visitU8(byte value, RecordConsumer recordConsumer) {
    return visitI32(UnsignedByte.toInt(value), recordConsumer);
  }

  @Override
  public Void visitU16(short value, RecordConsumer recordConsumer) {
    return visitI32(UnsignedShort.toInt(value), recordConsumer);
  }

  @Override
  public Void visitU32(int value, RecordConsumer recordConsumer) {
    return visitI32(Math.toIntExact(UnsignedInteger.toLong(value)), recordConsumer);
  }

  @Override
  public Void visitI32(int value, RecordConsumer recordConsumer) {
    recordConsumer.addInteger(value);
    return null;
  }

  @Override
  public Void visitInvalid(RecordConsumer recordConsumer) {
    return null;
  }
}
