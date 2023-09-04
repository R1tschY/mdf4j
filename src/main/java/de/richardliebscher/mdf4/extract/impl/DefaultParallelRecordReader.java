/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.Result.Err;
import de.richardliebscher.mdf4.Result.Ok;
import de.richardliebscher.mdf4.blocks.BlockType;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataBlock;
import de.richardliebscher.mdf4.blocks.DataZipped;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.DetachedRecordReader;
import de.richardliebscher.mdf4.extract.ParallelRecordReader;
import de.richardliebscher.mdf4.extract.RecordReader;
import de.richardliebscher.mdf4.extract.SerializableRecordFactory;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.RecordByteBuffer;
import de.richardliebscher.mdf4.extract.read.SerializableReadInto;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
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
class DefaultParallelRecordReader<B, R> implements ParallelRecordReader<B, R> {

  private final FileContext ctx;
  private final List<SerializableReadInto<B>> channelReaders;
  private final SerializableRecordFactory<B, R> factory;
  private final long[] dataList;
  private final long[] offsets;
  private final ChannelGroupBlock channelGroup;

  @Override
  public Stream<Result<R, IOException>> stream() {
    final var input = ctx.getInput();

    return StreamSupport.stream(new RecordSpliterator<>(
        channelReaders, factory,
        input, channelGroup.getDataBytes() + channelGroup.getInvalidationBytes(),
        dataList, offsets, channelGroup.getCycleCount()), false);
  }

  @AllArgsConstructor
  private static final class RecordSpliterator<B, R> implements
      Spliterator<Result<R, IOException>> {

    private final ByteInput input;
    private final List<SerializableReadInto<B>> channelReaders;
    private final SerializableRecordFactory<B, R> recordFactory;
    private final long[] dataList;
    private final long[] offsets;
    private final int recordSize;
    private final long estimatedCyclesPerBlock;
    private RecordBuffer recordInput;

    private int index;
    private final int end;
    private ReadableByteChannel currentBlock;
    private long remainingDataLength;
    private long readCycles;
    private int characteristics;

    public RecordSpliterator(List<SerializableReadInto<B>> channelReaders,
        SerializableRecordFactory<B, R> recordFactory,
        ByteInput input, int recordSize, long[] dataList, long[] offsets, long cycles) {
      this.channelReaders = channelReaders;
      this.recordFactory = recordFactory;
      this.recordSize = recordSize;
      this.dataList = dataList;
      this.offsets = offsets;
      this.estimatedCyclesPerBlock =
          Math.max(1, (int) Math.ceil(cycles / (dataList.length - 0.49)));

      this.recordInput = new RecordByteBuffer(
          ByteBuffer.allocate(recordSize), offsets[0] / recordSize);
      this.input = input;
      this.index = 0;
      this.end = dataList.length;
      this.readCycles = 0;
      this.characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE
          | Spliterator.SIZED; // TODO: SUBSIZED possible when EQUAL_LENGTH flag is set
    }

    public RecordSpliterator(RecordSpliterator<B, R> origin, int splitPos) throws IOException {
      this.channelReaders = origin.channelReaders;
      this.recordFactory = origin.recordFactory;
      this.dataList = origin.dataList;
      this.offsets = origin.offsets;
      this.recordSize = origin.recordSize;
      this.estimatedCyclesPerBlock = origin.estimatedCyclesPerBlock;

      this.input = origin.input.dup();
      this.index = origin.index;
      this.end = splitPos;
      this.currentBlock = origin.currentBlock;
      this.recordInput = origin.recordInput;
      origin.index = splitPos;
      origin.remainingDataLength = 0;
      origin.currentBlock = null;
      origin.recordInput = new RecordByteBuffer(
          ByteBuffer.allocate(recordSize), offsets[origin.index] / recordSize);

      this.readCycles = origin.readCycles;
      origin.readCycles = 0;

      this.characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
      origin.characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
    }

    public boolean read(Consumer<? super Result<R, IOException>> action) throws IOException {
      if (index == end) {
        return false;
      }

      if (currentBlock == null || remainingDataLength == 0) {
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
          currentBlock = dataBlock.getChannel(input);
          remainingDataLength = ((Data) dataBlock).getDataLength();
        } else if (dataBlock instanceof DataZipped) {
          final var dataZipped = (DataZipped) dataBlock;
          if (dataZipped.getOriginalBlockType().equals(BlockType.DT)) {
            currentBlock = dataZipped.getChannel(input);
            remainingDataLength = dataZipped.getOriginalDataLength();
          } else {
            throw new FormatException("Unexpected data block type in zipped data: "
                + dataZipped.getOriginalBlockType());
          }
        } else {
          throw new FormatException("Unexpected data block in data list: " + dataBlock);
        }

        if (remainingDataLength % recordSize != 0) {
          throw new FormatException("Data block size is not a multiple of the record size");
        }
      }

      recordInput.writeFully(currentBlock);
      remainingDataLength -= recordSize;

      final var recordBuilder = recordFactory.createRecordBuilder();
      for (var channelReader : channelReaders) {
        channelReader.readInto(recordInput, recordBuilder);
      }
      recordInput.incRecordIndex();
      action.accept(new Ok<>(recordFactory.finishRecord(recordBuilder)));

      readCycles += 1;
      return true;
    }

    @Override
    public RecordSpliterator<B, R> trySplit() {
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
  public List<DetachedRecordReader<B, R>> splitIntoDetached(int parts) {
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

  private DetachedRecordReader<B, R> newDetachedRecordReader(long[] dataListPart, long[] offsets) {
    return new MyDetachedRecordReader<>(
        dataListPart, offsets, channelReaders, factory,
        channelGroup.getDataBytes() + channelGroup.getInvalidationBytes());
  }

  private static class MyRecordReader<B, R> implements RecordReader<B, R> {

    private final ByteInput input;
    private final List<SerializableReadInto<B>> channelReaders;
    private final SerializableRecordFactory<B, R> recordFactory;
    private final long[] dataList;
    private final int recordSize;
    private final RecordBuffer recordInput;
    private int index;
    private ReadableByteChannel currentBlock;
    private long remainingDataLength;

    public MyRecordReader(
        ByteInput input, List<SerializableReadInto<B>> channelReaders,
        SerializableRecordFactory<B, R> recordFactory, int recordSize, long[] dataListPart,
        long[] offsets) {
      this.input = input;
      this.channelReaders = channelReaders;
      this.recordFactory = recordFactory;
      this.dataList = dataListPart;
      this.recordSize = recordSize;
      this.index = 0;
      this.recordInput = new RecordByteBuffer(
          ByteBuffer.allocate(recordSize), offsets[index] / recordSize);
    }

    @Override
    public boolean hasNext() throws IOException {
      return ensureNextBlock();
    }

    @Override
    public R next() throws IOException, NoSuchElementException {
      prepareRead();

      final var recordBuilder = recordFactory.createRecordBuilder();
      for (var channelReader : channelReaders) {
        channelReader.readInto(recordInput, recordBuilder);
      }
      finishRead();
      return recordFactory.finishRecord(recordBuilder);
    }

    @Override
    public void nextInto(B destination) throws IOException, NoSuchElementException {
      prepareRead();

      for (var channelReader : channelReaders) {
        channelReader.readInto(recordInput, destination);
      }
      finishRead();
    }

    private boolean ensureNextBlock() throws IOException {
      while (currentBlock == null || remainingDataLength == 0) {
        if (index == dataList.length) {
          return false;
        }

        final var dataBlock = Link.<DataBlock>of(dataList[index])
            .resolveNonCached(DataBlock.META, input)
            .orElseThrow(() -> new FormatException("Data link in DL block should not be NIL"));
        if (dataBlock instanceof Data) {
          currentBlock = dataBlock.getChannel(input);
          remainingDataLength = ((Data) dataBlock).getDataLength();
        } else if (dataBlock instanceof DataZipped) {
          final var dataZipped = (DataZipped) dataBlock;
          if (dataZipped.getOriginalBlockType().equals(BlockType.DT)) {
            currentBlock = dataZipped.getChannel(input);
            remainingDataLength = dataZipped.getOriginalDataLength();
          } else {
            throw new FormatException("Unexpected data block type in zipped data: "
                + dataZipped.getOriginalBlockType());
          }
        } else {
          throw new FormatException("Unexpected data block in data list: " + dataBlock);
        }

        if (remainingDataLength % recordSize != 0) {
          throw new NotImplementedFeatureException(
              "Data block size is not a multiple of the record size");
        }
        index += 1;
      }

      return true;
    }

    private void prepareRead() throws IOException {
      if (!ensureNextBlock()) {
        throw new NoSuchElementException();
      }

      recordInput.writeFully(currentBlock);
      remainingDataLength -= recordSize;
    }

    private void finishRead() {
      recordInput.incRecordIndex();
    }
  }

  private static class MyDetachedRecordReader<B, R> implements DetachedRecordReader<B, R> {

    private final long[] dataListPart;
    private final long[] offsets;
    private final List<SerializableReadInto<B>> channelReaders;
    private final SerializableRecordFactory<B, R> recordDeserializer;
    private final int recordSize;

    public MyDetachedRecordReader(long[] dataListPart, long[] offsets,
        List<SerializableReadInto<B>> channelReaders,
        SerializableRecordFactory<B, R> recordDeserializer,
        int recordSize) {
      this.dataListPart = dataListPart;
      this.offsets = offsets;
      this.channelReaders = channelReaders;
      this.recordDeserializer = recordDeserializer;
      this.recordSize = recordSize;
    }

    @Override
    public RecordReader<B, R> attach(FileContext ctx) {
      return new MyRecordReader<>(
          ctx.getInput(),
          channelReaders,
          recordDeserializer,
          recordSize,
          dataListPart, offsets);
    }
  }
}
