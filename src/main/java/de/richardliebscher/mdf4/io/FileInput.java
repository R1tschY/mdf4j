/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * File as input.
 */
public class FileInput implements ByteInput {

  private final Path path;
  private final FileChannel byteChannel;
  private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES)
      .order(ByteOrder.LITTLE_ENDIAN);

  /**
   * Construct from path.
   *
   * @param path Path
   * @throws IOException Unable to open file
   */
  public FileInput(Path path) throws IOException {
    this.path = path;
    this.byteChannel = FileChannel.open(path, StandardOpenOption.READ);
  }

  @Override
  public byte readU8() throws IOException {
    buffer.position(0);
    buffer.limit(Byte.BYTES);
    byteChannel.read(buffer);
    return buffer.get(0);
  }

  @Override
  public short readI16() throws IOException {
    buffer.position(0);
    buffer.limit(Short.BYTES);
    byteChannel.read(buffer);
    return buffer.getShort(0);
  }

  @Override
  public int readI32() throws IOException {
    buffer.position(0);
    buffer.limit(Integer.BYTES);
    byteChannel.read(buffer);
    return buffer.getInt(0);
  }

  @Override
  public long readI64() throws IOException {
    buffer.position(0);
    buffer.limit(Long.BYTES);
    byteChannel.read(buffer);
    return buffer.getLong(0);
  }

  @Override
  public float readF32() throws IOException {
    buffer.position(0);
    buffer.limit(Float.BYTES);
    byteChannel.read(buffer);
    return buffer.getFloat(0);
  }

  @Override
  public double readF64() throws IOException {
    buffer.position(0);
    buffer.limit(Double.BYTES);
    byteChannel.read(buffer);
    return buffer.getDouble(0);
  }

  @Override
  public String readString(int bytes, Charset charset) throws IOException {
    return new String(readBytes(bytes), charset);
  }

  @Override
  public void skip(int bytes) throws IOException {
    byteChannel.position(byteChannel.position() + bytes);
  }

  @Override
  public void seek(long pos) throws IOException {
    byteChannel.position(pos);
  }

  @Override
  public long pos() throws IOException {
    return byteChannel.position();
  }

  @Override
  public byte[] readBytes(int dataLength) throws IOException {
    final var buffer = ByteBuffer.wrap(new byte[dataLength]);
    byteChannel.read(buffer);
    return buffer.array();
  }

  @Override
  public ByteInput dup() throws IOException {
    return new FileInput(path);
  }
}
