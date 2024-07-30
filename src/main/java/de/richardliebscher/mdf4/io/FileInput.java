/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import static de.richardliebscher.mdf4.internal.ChannelSupport.readFully;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * File as input.
 */
public class FileInput implements ReadWrite {

  private final Path path;
  private final FileChannel byteChannel;
  private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES)
      .order(ByteOrder.LITTLE_ENDIAN);
  private final List<OpenOption> options;

  /**
   * Construct from path.
   *
   * @param path Path
   * @throws IOException Unable to open file
   */
  public FileInput(Path path, OpenOption... options) throws IOException {
    this.path = path;
    this.options = List.of(options);
    this.byteChannel = FileChannel.open(path, options);
  }

  @Override
  public byte readU8() throws IOException {
    buffer.position(0);
    buffer.limit(Byte.BYTES);
    readFully(byteChannel, buffer);
    return buffer.get(0);
  }

  @Override
  public short readI16() throws IOException {
    buffer.position(0);
    buffer.limit(Short.BYTES);
    readFully(byteChannel, buffer);
    return buffer.getShort(0);
  }

  @Override
  public int readI32() throws IOException {
    buffer.position(0);
    buffer.limit(Integer.BYTES);
    readFully(byteChannel, buffer);
    return buffer.getInt(0);
  }

  @Override
  public long readI64() throws IOException {
    buffer.position(0);
    buffer.limit(Long.BYTES);
    readFully(byteChannel, buffer);
    return buffer.getLong(0);
  }

  @Override
  public float readF32() throws IOException {
    buffer.position(0);
    buffer.limit(Float.BYTES);
    readFully(byteChannel, buffer);
    return buffer.getFloat(0);
  }

  @Override
  public double readF64() throws IOException {
    buffer.position(0);
    buffer.limit(Double.BYTES);
    readFully(byteChannel, buffer);
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
    readFully(byteChannel, buffer);
    return buffer.array();
  }

  @Override
  public InputStream getStream() {
    return Channels.newInputStream(byteChannel);
  }

  @Override
  public ReadableByteChannel getChannel() {
    return byteChannel;
  }

  @Override
  public FileInput dup() throws IOException {
    final var options = new ArrayList<OpenOption>();
    if (this.options.contains(StandardOpenOption.READ)) {
      options.add(StandardOpenOption.READ);
    }
    if (this.options.contains(StandardOpenOption.WRITE)) {
      options.add(StandardOpenOption.WRITE);
    }
    return new FileInput(path, options.toArray(new OpenOption[0]));
  }

  @Override
  public void close() throws IOException {
    byteChannel.close();
  }

  @Override
  public void writePadding(int size) throws IOException {
    buffer.clear();
    for (int i = 0; i < size; i++) {
      buffer.put((byte) 0);
    }
    buffer.flip();
    if (byteChannel.write(buffer) != size) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(byte value) throws IOException {
    buffer.clear();
    buffer.put(value);
    buffer.flip();
    if (byteChannel.write(buffer) != Byte.BYTES) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(short value) throws IOException {
    buffer.clear();
    buffer.putShort(value);
    buffer.flip();
    if (byteChannel.write(buffer) != Short.BYTES) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(int value) throws IOException {
    buffer.clear();
    buffer.putInt(value);
    buffer.flip();
    if (byteChannel.write(buffer) != Integer.BYTES) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(long value) throws IOException {
    buffer.clear();
    buffer.putLong(value);
    buffer.flip();
    if (byteChannel.write(buffer) != Long.BYTES) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(float value) throws IOException {
    buffer.clear();
    buffer.putFloat(value);
    buffer.flip();
    if (byteChannel.write(buffer) != Float.BYTES) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(double value) throws IOException {
    buffer.clear();
    buffer.putDouble(value);
    buffer.flip();
    if (byteChannel.write(buffer) != Double.BYTES) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(String value, Charset charset) throws IOException {
    final var bytes = value.getBytes(charset);
    if (byteChannel.write(ByteBuffer.wrap(bytes)) != bytes.length) {
      throw new IOException("Incomplete write");
    }
  }

  @Override
  public void write(byte[] value, int offset, int length) throws IOException {
    if (byteChannel.write(ByteBuffer.wrap(value, offset, length)) != length) {
      throw new IOException("Incomplete write");
    }
  }
}
