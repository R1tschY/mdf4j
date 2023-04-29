/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ByteBufferInput implements ByteInput {

  private final ByteBuffer buffer;

  public ByteBufferInput(ByteBuffer buffer) {
    this.buffer = buffer;
    this.buffer.order(ByteOrder.LITTLE_ENDIAN);
  }

  @Override
  public byte readU8() throws IOException {
    return buffer.get();
  }

  @Override
  public short readI16Le() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getShort();
  }

  @Override
  public int readI32Le() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getInt();
  }

  @Override
  public long readI64Le() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getLong();
  }

  @Override
  public float readF32Le() {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getFloat();
  }

  @Override
  public double readF64Le() throws IOException {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getDouble();
  }

  @Override
  public short readI16Be() throws IOException {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getShort();
  }

  @Override
  public int readI32Be() throws IOException {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getInt();
  }

  @Override
  public long readI64Be() throws IOException {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getLong();
  }

  @Override
  public float readF32Be() throws IOException {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getFloat();
  }

  @Override
  public double readF64Be() throws IOException {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getDouble();
  }

  @Override
  public String readString(int bytes, Charset charset) throws IOException {
    final var buf = new byte[bytes];
    buffer.get(buf);
    return new String(buf, charset);
  }

  @Override
  public void skip(int bytes) throws IOException {
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
  public byte[] readBytes(long dataLength) throws IOException {
    final var buf = new byte[Math.toIntExact(dataLength)];
    buffer.get(buf);
    return buf;
  }
}
