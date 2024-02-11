/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataStorage;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

public class DataStorageRead<T extends Data<T>> implements DataRead<T> {

  private final ByteInput input;
  private final DataStorage<T> storage;
  private long remainingDataLength;
  private ReadableByteChannel currentBlock;
  private boolean closed = false;

  public DataStorageRead(ByteInput input, DataStorage<T> storage) {
    this.input = input;
    this.storage = storage;
  }

  public DataStorageRead(ReadableByteChannel input, long remainingDataLength) {
    this(null, null);
    this.currentBlock = input;
    this.remainingDataLength = remainingDataLength;
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }

    if (currentBlock == null) {
      currentBlock = storage.getChannel(input);
      remainingDataLength = storage.getChannelLength();
    }

    if (remainingDataLength == 0) {
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
