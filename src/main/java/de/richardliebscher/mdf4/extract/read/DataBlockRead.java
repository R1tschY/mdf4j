/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.ChannelBlockData;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

public class DataBlockRead implements DataRead {

  private final ByteInput input;
  private final ChannelBlockData data;
  private long remainingDataLength;
  private ReadableByteChannel currentBlock;
  private boolean closed = false;

  public DataBlockRead(ByteInput input, ChannelBlockData data) {
    this.input = input;
    this.data = data;
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }

    if (currentBlock == null) {
      currentBlock = data.getChannel(input);
      remainingDataLength = data.getDataLength();
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
