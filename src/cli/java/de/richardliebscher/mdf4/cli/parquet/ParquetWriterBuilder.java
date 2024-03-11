/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.schema.MessageType;

public class ParquetWriterBuilder extends ParquetWriter.Builder<Record, ParquetWriterBuilder> {

  private MessageType schema = null;

  public ParquetWriterBuilder(Path file) {
    super(file);
  }

  public ParquetWriterBuilder withType(MessageType type) {
    this.schema = type;
    return this;
  }

  @Override
  protected ParquetWriterBuilder self() {
    return this;
  }

  @Override
  protected WriteSupport<Record> getWriteSupport(Configuration conf) {
    return new RecordWriteSupport(schema);
  }
}
