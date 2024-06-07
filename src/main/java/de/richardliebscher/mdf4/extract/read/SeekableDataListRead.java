/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.BlockType;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataStorage;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

public class SeekableDataListRead<T extends Data<T>> implements DataRead<T> {

  private final ByteInput input;
  private final DataList<T> dataList;
  private final BlockType<DataStorage<T>> storageBlockType;

  private int blockIndex = -1;
  private ReadableByteChannel blockChannel = null;
  private long remainingDataLength = 0;
  private boolean closed = false;
  private long pos = 0;

  public SeekableDataListRead(
      ByteInput input, DataList<T> dataList, BlockType<DataStorage<T>> storageBlockType) {
    this.input = input;
    this.dataList = dataList;
    this.storageBlockType = storageBlockType;
  }

  private void setBlockChannel(DataStorage<T> storage) throws IOException {
    blockChannel = storage.getChannel(input);
    remainingDataLength = storage.getChannelLength();
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }

    final var hasData = ensureDataStream();
    if (!hasData) {
      return -1;
    }

    final int remaining = (int) Math.min(remainingDataLength, dst.remaining());
    final var oldLimit = dst.limit();
    dst.limit(remaining);
    try {
      final var bytes = blockChannel.read(dst);
      if (bytes < 0) {
        throw new IllegalStateException("Unexpected end of stream");
      }
      pos += bytes;
      remainingDataLength -= bytes;
      return bytes;
    } finally {
      dst.limit(oldLimit);
    }
  }

  @Override
  public long position() {
    return pos;
  }

  @Override
  public SeekableByteChannel position(long newPosition) throws IOException {
    if (pos == newPosition) {
      return this;
    }

    final var oldBlockIndex = blockIndex;
    final var oldPosition = pos;

    pos = newPosition;
    blockIndex = dataList.getOffsets().indexOfPosition(newPosition);
    if (blockIndex < 0) {
      blockIndex = -1;
      remainingDataLength = 0;
    } else {
      final int toSkip;
      if (blockIndex != oldBlockIndex || newPosition < oldPosition) {
        final var offset = dataList.getOffsets().get(blockIndex);
        toSkip = Math.toIntExact(newPosition - offset);
        setBlockChannel(dataList.getDataBlocks().get(blockIndex).resolve(storageBlockType, input)
            .orElseThrow());
      } else {
        toSkip = Math.toIntExact(newPosition - oldPosition);
      }

      if (toSkip != 0) {
        // TODO: better skip
        blockChannel.read(ByteBuffer.allocate(toSkip));
        remainingDataLength -= toSkip;
      }
    }

    return this;
  }

  @Override
  public long size() throws IOException {
    final var dataBlocks = dataList.getDataBlocks();
    if (dataBlocks.isEmpty()) {
      return 0;
    } else {
      final var lastBlockLength = dataBlocks.get(dataBlocks.size() - 1)
          .resolve(storageBlockType, input)
          .orElseThrow(() -> new FormatException("Link to DT block can not be NIL"))
          .getChannelLength();
      return dataList.getOffsets().last() + lastBlockLength;
    }
  }

  private boolean ensureDataStream() throws IOException {
    if (remainingDataLength == 0) {
      final var dataBlocks = dataList.getDataBlocks();
      blockIndex += 1;
      if (blockIndex >= dataBlocks.size()) {
        return false;
      }

      setBlockChannel(dataBlocks.get(blockIndex).resolveNonCached(storageBlockType, input)
          .orElseThrow(() -> new FormatException("Data link in DL block should not be NIL")));
    }

    return true;
  }

  @Override
  public boolean isOpen() {
    return !closed;
  }

  @Override
  public void close() throws IOException {
    if (blockChannel != null) {
      blockChannel.close();
    }
    closed = true;
  }

  public SeekableDataListRead<T> dup() throws IOException {
    return new SeekableDataListRead<>(input.dup(), dataList, storageBlockType);
  }
}
