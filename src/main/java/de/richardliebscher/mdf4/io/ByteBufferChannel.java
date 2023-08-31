/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import lombok.NonNull;

class ByteBufferChannel implements SeekableByteChannel {

  private final ByteBuffer byteBuffer;
  private final ByteBuffer slice;
  private Long outOfStreamPosition = null;
  private boolean closed = false;

  public ByteBufferChannel(@NonNull ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
    this.slice = byteBuffer.slice();
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }

    if (byteBuffer.remaining() == 0 || outOfStreamPosition != null) {
      return -1;
    }

    final var n = Math.min(byteBuffer.remaining(), dst.remaining());
    slice.limit(slice.position() + n);
    dst.put(slice);
    byteBuffer.position(byteBuffer.position() + n);
    return n;
  }

  @Override
  public int write(ByteBuffer src) {
    throw new NonWritableChannelException();
  }

  @Override
  public long position() {
    return outOfStreamPosition != null ? outOfStreamPosition : byteBuffer.position();
  }

  @Override
  public SeekableByteChannel position(long newPosition) {
    if (newPosition < 0) {
      throw new IllegalArgumentException("New position should not be negative");
    }
    if (newPosition > byteBuffer.limit()) {
      outOfStreamPosition = newPosition;
    }
    byteBuffer.position((int) newPosition);
    return this;
  }

  @Override
  public long size() throws IOException {
    return byteBuffer.limit();
  }

  @Override
  public SeekableByteChannel truncate(long size) {
    throw new NonWritableChannelException();
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
