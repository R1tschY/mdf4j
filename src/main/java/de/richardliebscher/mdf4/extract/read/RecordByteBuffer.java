/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordByteBuffer implements RecordBuffer {
  private final ByteBuffer buffer;

  @Override
  public byte readU8(int pos) {
    return buffer.get(pos);
  }

  @Override
  public short readI16Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getShort(pos);
  }

  @Override
  public int readI32Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getInt(pos);
  }

  @Override
  public long readI64Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getLong(pos);
  }

  @Override
  public float readF32Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getFloat(pos);
  }

  @Override
  public double readF64Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getDouble(pos);
  }

  @Override
  public short readI16Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getShort(pos);
  }

  @Override
  public int readI32Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getInt(pos);
  }

  @Override
  public long readI64Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getLong(pos);
  }

  @Override
  public float readF32Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getFloat(pos);
  }

  @Override
  public double readF64Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getDouble(pos);
  }

  @Override
  public String readString(int pos, int bytes, Charset charset) {
    final var buf = new byte[bytes];
    buffer.get(buf);
    return new String(buf, charset);
  }

  @Override
  public void readBytes(int pos, byte[] bytes) {
    buffer.get(bytes);
  }
}
