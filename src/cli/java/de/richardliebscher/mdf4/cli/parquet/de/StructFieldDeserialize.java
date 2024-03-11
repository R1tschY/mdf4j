/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet.de;

import de.richardliebscher.mdf4.datatypes.StructType;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.StructAccess;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.parquet.io.api.RecordConsumer;

@RequiredArgsConstructor
@Getter
final class StructFieldDeserialize implements DeserializeInto<RecordConsumer>,
    de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

  private final String[] fieldNames;
  private final DeserializeInto<RecordConsumer>[] fieldDe;

  @SuppressWarnings("unchecked")
  public StructFieldDeserialize(StructType type) throws IOException {
    final var fieldCount = type.fields().size();
    fieldNames = new String[fieldCount];
    fieldDe = (DeserializeInto<RecordConsumer>[]) new DeserializeInto<?>[fieldCount];

    final var fields = type.fields();
    for (int i = 0; i < fieldCount; i++) {
      final var field = fields.get(i);
      fieldNames[i] = field.name();
      fieldDe[i] = DeserializeIntoParquet.forType(field.dataType());
    }
  }

  @Override
  public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
      throws IOException {
    deserializer.deserialize_value(this, recordConsumer);
  }

  @Override
  public String expecting() {
    return "structure";
  }

  @Override
  public Void visitStruct(StructAccess struct, RecordConsumer recordConsumer) throws IOException {
    final var fields = struct.fields();
    recordConsumer.startGroup();
    for (int i = 0; i < fields; i++) {
      recordConsumer.startField(fieldNames[i], i);
      struct.next_field(fieldDe[i], recordConsumer);
      recordConsumer.endField(fieldNames[i], i);
    }
    recordConsumer.endGroup();
    return null;
  }

  @Override
  public Void visitInvalid(RecordConsumer recordConsumer) {
    return null;
  }
}
