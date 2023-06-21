/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.Result.Err;
import de.richardliebscher.mdf4.Result.Ok;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataBlock;
import de.richardliebscher.mdf4.blocks.DataZipped;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.DetachedRecordReader;
import de.richardliebscher.mdf4.extract.ParallelRecordReader;
import de.richardliebscher.mdf4.extract.RecordReader;
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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class DefaultParallelRecordReader<R> implements ParallelRecordReader<R> {

  private final FileContext ctx;
  private final List<ValueRead> channelReaders;
  private final SerializableRecordVisitor<R> recordDeserializer;
  private final long[] dataList;
  private final long[] offsets;
  private final ChannelGroupBlock channelGroup;

  @Override
  public Stream<Result<R, IOException>> stream() {
    final var input = ctx.getInput();

    return StreamSupport.stream(new RecordSpliterator<>(
        channelReaders, recordDeserializer,
        input, channelGroup.getDataBytes() + channelGroup.getInvalidationBytes(),
        dataList, offsets, channelGroup.getCycleCount()), false);
  }

  @AllArgsConstructor
  private static final class RecordSpliterator<R> implements Spliterator<Result<R, IOException>> {

    private final ByteInput input;
    private final List<ValueRead> channelReaders;
    private final SerializableRecordVisitor<R> rowDeserializer;
    private final long[] dataList;
    private final long[] offsets;
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
        ByteInput input, int recordSize, long[] dataList, long[] offsets, long cycles) {
      this.channelReaders = channelReaders;
      this.rowDeserializer = rowDeserializer;
      this.recordSize = recordSize;
      this.dataList = dataList;
      this.offsets = offsets;
      this.estimatedCyclesPerBlock =
          Math.max(1, (int) Math.ceil(cycles / (dataList.length - 0.49)));

      this.input = input;
      this.index = 0;
      this.end = dataList.length;
      this.readCycles = 0;
      this.characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE
          | Spliterator.SIZED; // TODO: SUBSIZED possible when EQUAL_LENGTH flag is set
    }

    public RecordSpliterator(RecordSpliterator<R> origin, int splitPos) throws IOException {
      this.channelReaders = origin.channelReaders;
      this.rowDeserializer = origin.rowDeserializer;
      this.dataList = origin.dataList;
      this.offsets = origin.offsets;
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

    public boolean read(Consumer<? super Result<R, IOException>> action) throws IOException {
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
        recordInput = new RecordByteBuffer(currentBlock, offsets[index] / recordSize);
      }

      action.accept(new Ok<>(rowDeserializer.visitRecord(new RecordAccess() {
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
      })));

      readCycles += 1;
      recordInput.incRecordIndex();
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
        try {
          return new RecordSpliterator<>(this, middle);
        } catch (IOException e) {
          return null;
        }
      }
    }

    @Override
    public boolean tryAdvance(Consumer<? super Result<R, IOException>> action) {
      try {
        return read(action);
      } catch (IOException e) {
        action.accept(new Err<>(e));
        index = end;
        return true;
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

  @Override
  public List<DetachedRecordReader<R>> splitIntoDetached(int parts) {
    if (parts < 1) {
      throw new IllegalArgumentException("parts should be greater than or equal to 1");
    }

    if (dataList.length <= parts) {
      return IntStream.range(0, dataList.length)
          .mapToObj(i -> newDetachedRecordReader(new long[]{dataList[i]}, new long[]{offsets[i]}))
          .collect(Collectors.toList());
    } else {
      final double partLength = dataList.length / (double) parts;
      return IntStream.range(0, parts)
          .mapToObj(partIndex -> {
            final var start = (int) Math.round(partIndex * partLength);
            final var end = (int) Math.round((partIndex + 1) * partLength);
            return newDetachedRecordReader(
                Arrays.copyOfRange(dataList, start, end),
                Arrays.copyOfRange(offsets, start, end));
          })
          .collect(Collectors.toList());
    }
  }

  private DetachedRecordReader<R> newDetachedRecordReader(long[] dataListPart, long[] offsets) {
    return new MyDetachedRecordReader<>(
        dataListPart, offsets, channelReaders, recordDeserializer,
        channelGroup.getDataBytes() + channelGroup.getInvalidationBytes());
  }

  private static class MyRecordReader<R> implements RecordReader<R> {

    private final ByteInput input;
    private final List<ValueRead> channelReaders;
    private final SerializableRecordVisitor<R> recordDeserializer;
    private final long[] dataList;
    private final long[] offsets;
    private final int recordSize;
    private RecordBuffer recordInput;
    private int index;
    private ByteBuffer currentBlock;

    public MyRecordReader(
        ByteInput input, List<ValueRead> channelReaders,
        SerializableRecordVisitor<R> recordDeserializer, int recordSize, long[] dataListPart,
        long[] offsets) {
      this.input = input;
      this.channelReaders = channelReaders;
      this.recordDeserializer = recordDeserializer;
      this.dataList = dataListPart;
      this.offsets = offsets;
      this.recordSize = recordSize;
      this.index = 0;
    }

    @Override
    public boolean hasNext() throws IOException {
      return ensureNextBlock();
    }

    @Override
    public R next() throws IOException, NoSuchElementException {
      if (!ensureNextBlock()) {
        throw new NoSuchElementException();
      }

      final var record = recordDeserializer.visitRecord(new RecordAccess() {
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
      });

      recordInput.incRecordIndex();
      currentBlock.position(currentBlock.position() + recordSize);
      return record;
    }

    private boolean ensureNextBlock() throws IOException {
      while (currentBlock == null || currentBlock.remaining() == 0) {
        if (index == dataList.length) {
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
          throw new NotImplementedFeatureException(
              "Data block size is not a multiple of the record size");
        }
        recordInput = new RecordByteBuffer(currentBlock, offsets[index] / recordSize);
        index += 1;
      }

      return true;
    }
  }

  private static class MyDetachedRecordReader<R> implements DetachedRecordReader<R> {

    private final long[] dataListPart;
    private final long[] offsets;
    private final List<ValueRead> channelReaders;
    private final SerializableRecordVisitor<R> recordDeserializer;
    private final int recordSize;

    public MyDetachedRecordReader(long[] dataListPart, long[] offsets,
        List<ValueRead> channelReaders, SerializableRecordVisitor<R> recordDeserializer,
        int recordSize) {
      this.dataListPart = dataListPart;
      this.offsets = offsets;
      this.channelReaders = channelReaders;
      this.recordDeserializer = recordDeserializer;
      this.recordSize = recordSize;
    }

    @Override
    public RecordReader<R> attach(FileContext ctx) {
      return new MyRecordReader<>(
          ctx.getInput(),
          channelReaders,
          recordDeserializer,
          recordSize,
          dataListPart, offsets);
    }
  }
}
