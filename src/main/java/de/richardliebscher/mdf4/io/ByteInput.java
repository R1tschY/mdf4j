/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

/**
 * Read MDF4 input file.
 */
public interface ByteInput extends Closeable {

  byte readU8() throws IOException;

  short readI16() throws IOException;

  int readI32() throws IOException;

  long readI64() throws IOException;

  float readF32() throws IOException;

  double readF64() throws IOException;

  String readString(int bytes, Charset charset) throws IOException;

  void skip(int bytes) throws IOException;

  void seek(long pos) throws IOException;

  long pos() throws IOException;

  byte[] readBytes(int dataLength) throws IOException;

  InputStream getStream();

  ReadableByteChannel getChannel();

  ByteInput dup() throws IOException;
}
