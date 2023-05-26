/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.Result.Err;
import de.richardliebscher.mdf4.Result.Ok;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeSeed;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.DataRead;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.RecordByteBuffer;
import de.richardliebscher.mdf4.extract.read.ValueRead;
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
public class RecordReader<R> {

  private final List<ValueRead> channelReaders;
  private final RecordVisitor<R> factory;
  private final DataRead dataSource;
  private final ByteBuffer buffer;
  private final ChannelGroupBlock group;
  private final RecordBuffer input;
  private long cycle = 0;

  RecordReader(
      List<ValueRead> channelReaders, RecordVisitor<R> factory, DataRead dataSource,
      ChannelGroupBlock group) {
    this.channelReaders = channelReaders;
    this.factory = factory;
    this.dataSource = dataSource;
    this.buffer = ByteBuffer.allocate(group.getDataBytes() + group.getInvalidationBytes());
    this.input = new RecordByteBuffer(buffer);
    this.group = group;
  }

  // PUBLIC

  /**
   * Get number of records.
   *
   * @return number of records
   */
  public long size() {
    return group.getCycleCount();
  }

  /**
   * Get number of remaining records.
   *
   * @return number of remaining records
   */
  public long remaining() {
    return group.getCycleCount() - cycle;
  }

  /**
   * Create an iterator for deserializing all elements the same way.
   *
   * @return Deserialized value
   */
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
          return new Ok<>(RecordReader.this.next());
        } catch (IOException exp) {
          return new Err<>(exp);
        }
      }
    };
  }

  /**
   * Read next record.
   *
   * @return Deserialized record
   * @throws IOException            Unable to read record from file
   * @throws NoSuchElementException No remaining records
   * @see #remaining()
   */
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

    return factory.visitRecord(new RecordAccess() {
      private int index = 0;
      private final int size = channelReaders.size();

      @Override
      public <S extends DeserializeSeed<T>, T> T nextElementSeed(Deserialize<T> deserialize, S seed)
          throws IOException {
        if (index >= size) {
          throw new NoSuchElementException();
        }

        final var ret = seed.deserialize(deserialize, new Deserializer() {
          @Override
          public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
            return channelReaders.get(index).read(input, visitor);
          }
        });
        index += 1;
        return ret;
      }

      @Override
      public int remaining() {
        return channelReaders.size() - index;
      }
    });
  }
}
