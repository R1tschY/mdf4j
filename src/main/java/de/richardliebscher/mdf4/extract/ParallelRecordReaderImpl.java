/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataBlock;
import de.richardliebscher.mdf4.blocks.DataZipped;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeSeed;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.SerializableRecordVisitor;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.RecordByteBuffer;
import de.richardliebscher.mdf4.extract.read.ValueRead;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ParallelRecordReaderImpl<R> implements ParallelRecordReader<R> {
  private final FileContext ctx;
  private final List<ValueRead> channelReaders;
  private final SerializableRecordVisitor<R> rowDeserializer;
  private final long[] dataList;
  private final ChannelGroupBlock channelGroup;

  @Override
  public Stream<R> stream() {
    final var input = ctx.getInput();

    return StreamSupport.stream(new RecordSpliterator<>(
            channelReaders, rowDeserializer,
            input, channelGroup.getDataBytes() + channelGroup.getInvalidationBytes(),
            dataList, channelGroup.getCycleCount()), false);
  }

  @AllArgsConstructor
  private static final class RecordSpliterator<R> implements Spliterator<R> {
    private final ByteInput input;
    private final List<ValueRead> channelReaders;
    private final SerializableRecordVisitor<R> rowDeserializer;
    private final long[] dataList;
    private final int recordSize;
    private final long estimatedCyclesPerBlock;
    private RecordBuffer recordInput;

    private int index;
    private final int end;
    private ByteBuffer currentBlock;
    private long readCycles;
    private int characteristics;

    public RecordSpliterator(List<ValueRead> channelReaders,
                             SerializableRecordVisitor<R> rowDeserializer,
                             ByteInput input, int recordSize, long[] dataList, long cycles) {
      this.channelReaders = channelReaders;
      this.rowDeserializer = rowDeserializer;
      this.recordSize = recordSize;
      this.dataList = dataList;
      this.estimatedCyclesPerBlock =
              Math.max(1, (int) Math.ceil(cycles / (dataList.length - 0.49)));

      this.input = input;
      this.index = 0;
      this.end = dataList.length;
      this.readCycles = 0;
      this.characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE
              | Spliterator.SIZED; // TODO: SUBSIZED possible when EQUAL_LENGTH flag is set
    }

    public RecordSpliterator(RecordSpliterator<R> origin, int splitPos) {
      this.channelReaders = origin.channelReaders;
      this.rowDeserializer = origin.rowDeserializer;
      this.dataList = origin.dataList;
      this.recordSize = origin.recordSize;
      this.estimatedCyclesPerBlock = origin.estimatedCyclesPerBlock;

      this.input = origin.input.dup();
      this.index = origin.index;
      origin.index = splitPos;
      this.end = splitPos;
      this.currentBlock = origin.currentBlock;
      this.recordInput = origin.recordInput;
      origin.currentBlock = null;
      origin.recordInput = null;

      this.readCycles = origin.readCycles;
      origin.readCycles = 0;

      this.characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
      origin.characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
    }

    public boolean read(Consumer<? super R> action) throws IOException {
      if (index == end) {
        return false;
      }

      if (currentBlock == null || currentBlock.remaining() == 0) {
        if (currentBlock != null) {
          index += 1;
          readCycles = 0;
        }
        if (index == end) {
          return false;
        }

        final var dataBlock = Link.<DataBlock>of(dataList[index])
                .resolveNonCached(DataBlock.META, input)
                .orElseThrow(() -> new FormatException("Data link in DL block should not be NIL"));
        if (dataBlock instanceof Data) {
          currentBlock = ByteBuffer.wrap(((Data) dataBlock).getData());
        } else if (dataBlock instanceof DataZipped) {
          final var uncompressedDataBlock = ((DataZipped) dataBlock).getUncompressed();
          if (uncompressedDataBlock instanceof Data) {
            currentBlock = ByteBuffer.wrap(((Data) uncompressedDataBlock).getData());
          } else {
            throw new FormatException(
                    "Unexpected data block in zipped data: " + uncompressedDataBlock);
          }
        } else {
          throw new FormatException("Unexpected data block in data list: " + dataBlock);
        }

        if (currentBlock.remaining() % recordSize != 0) {
          throw new FormatException("Data block size is not a multiple of the record size");
        }
        recordInput = new RecordByteBuffer(currentBlock);
      }

      action.accept(rowDeserializer.visitRecord(new RecordAccess() {
        private int index = 0;
        private final int size = channelReaders.size();

        @Override
        public <S extends DeserializeSeed<T>, T> T nextElementSeed(
                Deserialize<T> deserialize, S seed) throws IOException {
          if (index >= size) {
            throw new NoSuchElementException();
          }

          final var ret = seed.deserialize(deserialize, new Deserializer() {
            @Override
            public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
              return channelReaders.get(index).read(recordInput, visitor);
            }
          });
          index += 1;
          return ret;
        }

        @Override
        public int remaining() {
          return channelReaders.size() - index;
        }
      }));

      readCycles += 1;
      currentBlock.position(currentBlock.position() + recordSize);
      return true;
    }

    @Override
    public RecordSpliterator<R> trySplit() {
      int start = index;
      int middle = start + (end - start) >>> 1;


      if (start >= middle) {
        return null;
      } else {
        return new RecordSpliterator<>(this, middle);
      }
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
      try {
        return read(action);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public long estimateSize() {
      if (index == end) {
        return 0;
      }

      final var nextBlocks = (end - index) - 1;

      return nextBlocks * estimatedCyclesPerBlock
              + Math.max(1, estimatedCyclesPerBlock - readCycles);
    }

    @Override
    public int characteristics() {
      return characteristics;
    }
  }
}
