/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

/**
 * Input file as byte buffer.
 */
public class ByteBufferInput implements ReadWrite {

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
    return buffer.getShort();
  }

  @Override
  public int readI32() {
    return buffer.getInt();
  }

  @Override
  public long readI64() {
    return buffer.getLong();
  }

  @Override
  public float readF32() {
    return buffer.getFloat();
  }

  @Override
  public double readF64() {
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
  public void close() {
    /* do nothong */
  }

  @Override
  public void writePadding(int size) {
    for (int i = 0; i < size; i++) {
      buffer.put((byte) 0);
    }
  }

  @Override
  public void write(byte value) {
    buffer.put(value);
  }

  @Override
  public void write(short value) {
    buffer.putShort(value);
  }

  @Override
  public void write(int value) {
    buffer.putInt(value);
  }

  @Override
  public void write(long value) {
    buffer.putLong(value);
  }

  @Override
  public void write(float value) {
    buffer.putFloat(value);
  }

  @Override
  public void write(double value) {
    buffer.putDouble(value);
  }

  @Override
  public void write(String value, Charset charset) {
    buffer.put(value.getBytes(charset));
  }

  @Override
  public void write(byte[] value, int offset, int length) {
    buffer.put(value, offset, length);
  }
}
