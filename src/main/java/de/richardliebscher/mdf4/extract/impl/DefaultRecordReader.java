/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.Result.Err;
import de.richardliebscher.mdf4.Result.Ok;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.SizedRecordReader;
import de.richardliebscher.mdf4.extract.read.DataRead;
import de.richardliebscher.mdf4.extract.read.ReadInto;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.RecordByteBuffer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.java.Log;

/**
 * Read records into user-defined type.
 *
 * @param <R> User-defined record type
 * @see de.richardliebscher.mdf4.Mdf4File#newRecordReader
 */
@Log
public class DefaultRecordReader<B, R> implements SizedRecordReader<R> {

  private final List<ReadInto<B>> channelReaders;
  private final RecordFactory<B, R> factory;
  private final DataRead dataSource;
  private final ByteBuffer buffer;
  private final ChannelGroupBlock group;
  private final RecordBuffer input;
  private long cycle = 0;

  DefaultRecordReader(
      List<ReadInto<B>> channelReaders,
      RecordFactory<B, R> factory, DataRead dataSource,
      ChannelGroupBlock group) {
    this.channelReaders = channelReaders;
    this.factory = factory;
    this.dataSource = dataSource;
    this.buffer = ByteBuffer.allocate(group.getDataBytes() + group.getInvalidationBytes());
    this.input = new RecordByteBuffer(buffer, 0);
    this.group = group;
  }

  // PUBLIC

  @Override
  public long size() {
    return group.getCycleCount();
  }

  @Override
  public long remaining() {
    return group.getCycleCount() - cycle;
  }

  @Override
  public Iterator<Result<R, IOException>> iterator() {
    return new Iterator<>() {
      private long index = 0;
      private final long size = size();

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public Result<R, IOException> next() {
        try {
          index += 1;
          return new Ok<>(DefaultRecordReader.this.next());
        } catch (IOException exp) {
          return new Err<>(exp);
        }
      }
    };
  }

  @Override
  public boolean hasNext() {
    return cycle < group.getCycleCount();
  }

  @Override
  public R next() throws IOException, NoSuchElementException {
    if (cycle >= group.getCycleCount()) {
      throw new NoSuchElementException();
    }

    cycle += 1;
    buffer.clear();
    final var bytes = dataSource.read(buffer);
    if (bytes != buffer.capacity()) {
      throw new FormatException(
          "Early end of data at cycle " + cycle + " of " + group.getCycleCount());
    }

    final B recordBuilder = factory.createRecordBuilder();
    for (var channelReader : channelReaders) {
      channelReader.readInto(input, recordBuilder);
    }

    input.incRecordIndex();
    return factory.finishRecord(recordBuilder);
  }

}
