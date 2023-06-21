/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import java.nio.charset.Charset;

/**
 * Read input file.
 */
public interface RecordBuffer {

  void incRecordIndex();

  long getRecordIndex();

  byte readU8(int pos);

  short readI16Le(int pos);

  int readI32Le(int pos);

  long readI64Le(int pos);

  float readF32Le(int pos);

  double readF64Le(int pos);

  short readI16Be(int pos);

  int readI32Be(int pos);

  long readI64Be(int pos);

  float readF32Be(int pos);

  double readF64Be(int pos);

  String readString(int pos, int bytes, Charset charset);

  void readBytes(int pos, byte[] buf);
}
