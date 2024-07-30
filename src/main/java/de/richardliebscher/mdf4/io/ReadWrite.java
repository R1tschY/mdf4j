/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import de.richardliebscher.mdf4.blocks.WriteData;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Interface for reading and writing MDF binary data.
 */
public interface ReadWrite extends ByteInput {
  void writePadding(int size) throws IOException;

  void write(byte value) throws IOException;

  void write(short value) throws IOException;

  void write(int value) throws IOException;

  void write(long value) throws IOException;

  void write(float value) throws IOException;

  void write(double value) throws IOException;

  void write(String value, Charset charset) throws IOException;

  void write(byte[] value, int offset, int length) throws IOException;

  default void write(byte[] value) throws IOException {
    write(value, 0, value.length);
  }

  default void write(WriteData value) throws IOException {
    value.write(this);
  }
}
