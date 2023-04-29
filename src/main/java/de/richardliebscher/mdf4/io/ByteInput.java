/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.IOException;
import java.nio.charset.Charset;

public interface ByteInput {

  byte readU8() throws IOException;

  short readI16Le() throws IOException;

  int readI32Le() throws IOException;

  long readI64Le() throws IOException;

  float readF32Le() throws IOException;

  double readF64Le() throws IOException;

  short readI16Be() throws IOException;

  int readI32Be() throws IOException;

  long readI64Be() throws IOException;

  float readF32Be() throws IOException;

  double readF64Be() throws IOException;

  String readString(int bytes, Charset charset) throws IOException;

  void skip(int bytes) throws IOException;

  void seek(long pos) throws IOException;

  long pos();

  byte[] readBytes(long dataLength) throws IOException;
}
