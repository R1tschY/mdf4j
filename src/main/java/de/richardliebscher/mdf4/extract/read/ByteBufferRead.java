/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ByteBufferRead implements DataRead {

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
    data.position(data.position() + bytesToRead);
    return bytesToRead;
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
