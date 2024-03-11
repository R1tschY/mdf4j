/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet.de;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import java.io.IOException;
import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;

@RequiredArgsConstructor
@Getter
final class ByteArrayFieldDeserialize implements DeserializeInto<RecordConsumer>,
    de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

  @Override
  public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
      throws IOException {
    deserializer.deserialize_value(this, recordConsumer);
  }

  @Override
  public String expecting() {
    return "byte array";
  }

  @Override
  public Void visitByteArray(ByteBuffer bytes, RecordConsumer recordConsumer) {
    recordConsumer.addBinary(Binary.fromReusedByteBuffer(bytes));
    return null;
  }

  @Override
  public Void visitByteArray(byte[] bytes, int offset, int length,
      RecordConsumer recordConsumer) {
    recordConsumer.addBinary(Binary.fromReusedByteArray(bytes, offset, length));
    return null;
  }

  @Override
  public Void visitByteArray(byte[] bytes, RecordConsumer recordConsumer) {
    recordConsumer.addBinary(Binary.fromReusedByteArray(bytes));
    return null;
  }

  @Override
  public Void visitInvalid(RecordConsumer recordConsumer) {
    return null;
  }
}
