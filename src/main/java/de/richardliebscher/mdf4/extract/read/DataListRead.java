/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.BlockType;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataListBlock;
import de.richardliebscher.mdf4.blocks.DataStorage;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;

public class DataListRead<T extends Data<T>> implements DataRead<T> {

  private final ByteInput input;
  private DataListBlock<T> dataList;
  private long remainingDataLength;
  private ReadableByteChannel currentBlock;
  private boolean closed = false;
  private Iterator<Link<DataStorage<T>>> dataBlocks;
  private final BlockType<DataStorage<T>> storageBlockType;

  public DataListRead(ByteInput input, DataListBlock<T> firstDataList,
      BlockType<DataStorage<T>> storageBlockType) {
    this.input = input;
    this.dataList = firstDataList;
    this.dataBlocks = firstDataList.getData().iterator();
    this.storageBlockType = storageBlockType;
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
    final var bytes = currentBlock.read(dst.slice().limit(remaining));
    if (bytes > 0) {
      remainingDataLength -= bytes;
    }
    return bytes;
  }

  @Override
  public long position() throws IOException {
    throw new IOException("Unsupported");
  }

  @Override
  public SeekableByteChannel position(long newPosition) throws IOException {
    throw new IOException("Unsupported");
  }

  @Override
  public long size() throws IOException {
    throw new IOException("Unsupported");
  }

  private boolean ensureDataStream() throws IOException {
    if (remainingDataLength == 0) {
      if (dataBlocks == null || !dataBlocks.hasNext()) {
        dataList = dataList.getNextDataList().resolve(DataListBlock.type(), input).orElse(null);
        if (dataList == null) {
          return false;
        }
        dataBlocks = dataList.getData().iterator();
        if (!dataBlocks.hasNext()) {
          return ensureDataStream();
        }
      }

      final var storage = dataBlocks.next().resolveNonCached(storageBlockType, input)
          .orElseThrow(() -> new FormatException("Data link in DL block should not be NIL"));
      currentBlock = storage.getChannel(input);
      remainingDataLength = storage.getChannelLength();
    }

    return true;
  }

  @Override
  public boolean isOpen() {
    return !closed;
  }

  @Override
  public void close() throws IOException {
    if (currentBlock != null) {
      currentBlock.close();
    }
    closed = true;
  }

}
