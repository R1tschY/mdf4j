/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.cli.parquet.ParquetSchemaGenerator;
import de.richardliebscher.mdf4.cli.parquet.ParquetWriterBuilder;
import de.richardliebscher.mdf4.cli.parquet.de.DeserializeIntoParquet;
import de.richardliebscher.mdf4.cli.utils.IntCell;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.parquet.hadoop.ParquetFileWriter.Mode;
import org.apache.parquet.io.api.RecordConsumer;

/**
 * CLI to read and convert MDF4 files.
 */
public class Main {

  /**
   * Entrypoint.
   *
   * @param args Commandline arguments
   * @throws IOException                   I/O error
   * @throws ChannelGroupNotFoundException Channel group not found
   */
  public static void main(String[] args) throws IOException, ChannelGroupNotFoundException {
    final var sourcePath = Path.of(args[0]);
    final var targetPath = new org.apache.hadoop.fs.Path(args[1]);

    final var reader = Mdf4File.open(sourcePath);

    final var channelIndex = new IntCell();
    final var recordReader = reader.newRecordReader(new RecordFactory<RecordConsumer, Void>() {
      @Override
      public boolean selectGroup(DataGroup dataGroup, ChannelGroup group) {
        return true;
      }

      @Override
      public DeserializeInto<RecordConsumer> selectChannel(DataGroup dataGroup,
          ChannelGroup group, Channel channel) throws IOException {
        final var deserializeInto = DeserializeIntoParquet.forType(
            channel.getDataType());
        final var fieldName = channel.getName();
        final var fieldIndex = channelIndex.replace(channelIndex.get() + 1);
        return (deserializer, dest) -> {
          dest.startField(fieldName, fieldIndex);
          deserializeInto.deserializeInto(deserializer, dest);
          dest.endField(fieldName, fieldIndex);
        };
      }

      @Override
      public RecordConsumer createRecordBuilder() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Void finishRecord(RecordConsumer unfinishedRecord) {
        throw new UnsupportedOperationException();
      }
    });

    final var schema = new ParquetSchemaGenerator(
        recordReader.getDataGroup(),
        recordReader.getChannelGroup(),
        recordReader.getChannels()
    ).generateSchema();

    try (final var writer = new ParquetWriterBuilder(targetPath)
        .withWriteMode(Mode.OVERWRITE)
        .withValidation(true)
        .withType(schema)
        .build()) {
      System.out.printf("%s%n", recordReader.size());
      while (recordReader.hasNext()) {
        writer.write(recordReader::nextInto);
      }
      System.out.printf("%s%n", writer.getDataSize());
    }
  }
}
