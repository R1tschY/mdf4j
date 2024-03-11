/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

@RequiredArgsConstructor
public class RecordWriteSupport extends WriteSupport<Record> {

  private final MessageType schema;
  private RecordConsumer recordConsumer;

  @Override
  public WriteContext init(Configuration configuration) {
    return new WriteContext(schema, Map.of());
  }

  @Override
  public void prepareForWrite(RecordConsumer recordConsumer) {
    this.recordConsumer = recordConsumer;
  }

  @Override
  public void write(Record record) {
    recordConsumer.startMessage();
    try {
      record.writeInto(recordConsumer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    recordConsumer.endMessage();
  }
}
