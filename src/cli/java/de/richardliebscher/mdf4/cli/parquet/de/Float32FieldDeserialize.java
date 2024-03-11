/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet.de;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.parquet.io.api.RecordConsumer;

@RequiredArgsConstructor
@Getter
final class Float32FieldDeserialize implements DeserializeInto<RecordConsumer>,
    de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

  @Override
  public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
      throws IOException {
    deserializer.deserialize_value(this, recordConsumer);
  }

  @Override
  public String expecting() {
    return "32-bit float";
  }

  @Override
  public Void visitF32(float value, RecordConsumer recordConsumer) {
    recordConsumer.addFloat(value);
    return null;
  }

  @Override
  public Void visitInvalid(RecordConsumer recordConsumer) {
    return null;
  }
}
