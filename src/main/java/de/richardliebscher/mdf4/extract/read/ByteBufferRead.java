/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.Data;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ByteBufferRead<T extends Data<T>> implements DataRead<T> {

  private final ByteBuffer data; // TODO: make thread safe
  private boolean closed = false;

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }

    if (data.remaining() == 0) {
      return -1;
    }

    int bytesToRead = Math.min(data.remaining(), dst.remaining());
    dst.put(data.slice().limit(bytesToRead));
    dst.position(0);
    data.position(data.position() + bytesToRead);
    return bytesToRead;
  }

  @Override
  public long position() throws IOException {
    return data.position();
  }

  @Override
  public SeekableByteChannel position(long newPosition) throws IOException {
    data.position(Math.toIntExact(newPosition));
    return this;
  }

  @Override
  public long size() throws IOException {
    return data.limit();
  }

  @Override
  public boolean isOpen() {
    return !closed;
  }

  @Override
  public void close() {
    closed = true;
  }
}
