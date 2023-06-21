/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RecordByteBuffer implements RecordBuffer {
  private final ByteBuffer buffer;
  private long recordIndex = 0;

  @Override
  public void incRecordIndex() {
    recordIndex += 1;
  }

  @Override
  public long getRecordIndex() {
    return recordIndex;
  }

  @Override
  public byte readU8(int pos) {
    return buffer.get(buffer.position() + pos);
  }

  @Override
  public short readI16Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getShort(buffer.position() + pos);
  }

  @Override
  public int readI32Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getInt(buffer.position() + pos);
  }

  @Override
  public long readI64Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getLong(buffer.position() + pos);
  }

  @Override
  public float readF32Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getFloat(buffer.position() + pos);
  }

  @Override
  public double readF64Le(int pos) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getDouble(buffer.position() + pos);
  }

  @Override
  public short readI16Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getShort(buffer.position() + pos);
  }

  @Override
  public int readI32Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getInt(buffer.position() + pos);
  }

  @Override
  public long readI64Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getLong(buffer.position() + pos);
  }

  @Override
  public float readF32Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getFloat(buffer.position() + pos);
  }

  @Override
  public double readF64Be(int pos) {
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getDouble(buffer.position() + pos);
  }

  @Override
  public String readString(int pos, int bytes, Charset charset) {
    final var buf = new byte[bytes];
    buffer.slice().position(pos).slice().get(buf); // TODO: PERF
    return new String(buf, charset);
  }

  @Override
  public void readBytes(int pos, byte[] bytes) {
    buffer.slice().position(pos).slice().get(bytes); // TODO: PERF
  }
}
