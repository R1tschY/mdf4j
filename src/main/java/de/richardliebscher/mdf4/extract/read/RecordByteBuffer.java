/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import static de.richardliebscher.mdf4.internal.ChannelSupport.readFully;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
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
  public void writeFully(ReadableByteChannel channel) throws IOException {
    buffer.clear();
    readFully(channel, buffer);
  }

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
    buffer.slice().position(pos).slice().get(buf); // TODO: PERF
    return new String(buf, charset);
  }

  @Override
  public void readBytes(int pos, byte[] bytes) {
    buffer.slice().position(pos).slice().get(bytes); // TODO: PERF
  }
}
