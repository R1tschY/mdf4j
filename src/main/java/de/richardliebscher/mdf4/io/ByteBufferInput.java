/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

/**
 * Input file as byte buffer.
 */
public class ByteBufferInput implements ByteInput {

  private final ByteBuffer buffer;

  /**
   * Create from byte buffer.
   *
   * @param buffer byte buffer
   */
  public ByteBufferInput(ByteBuffer buffer) {
    this.buffer = buffer;
    this.buffer.order(ByteOrder.LITTLE_ENDIAN);
  }

  @Override
  public byte readU8() {
    return buffer.get();
  }

  @Override
  public short readI16() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getShort();
  }

  @Override
  public int readI32() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getInt();
  }

  @Override
  public long readI64() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getLong();
  }

  @Override
  public float readF32() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getFloat();
  }

  @Override
  public double readF64() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getDouble();
  }

  @Override
  public String readString(int bytes, Charset charset) {
    final var buf = new byte[bytes];
    buffer.get(buf);
    return new String(buf, charset);
  }

  @Override
  public void skip(int bytes) {
    buffer.position(buffer.position() + bytes);
  }

  @Override
  public void seek(long pos) {
    buffer.position(Math.toIntExact(pos));
  }

  @Override
  public long pos() {
    return buffer.position();
  }

  @Override
  public byte[] readBytes(int dataLength) {
    final var buf = new byte[dataLength];
    buffer.get(buf);
    return buf;
  }

  @Override
  public InputStream getStream() {
    // TODO: PERF: build an unsynchronized version
    return new ByteArrayInputStream(
        buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
  }

  @Override
  public ReadableByteChannel getChannel() {
    return new ByteBufferChannel(buffer);
  }

  @Override
  public ByteInput dup() {
    return new ByteBufferInput(buffer.duplicate());
  }

  @Override
  public void close() throws IOException {
    /* do nothong */
  }
}
