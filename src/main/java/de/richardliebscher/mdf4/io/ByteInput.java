/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.io;

import java.io.IOException;
import java.nio.charset.Charset;

public interface ByteInput {
    byte readU8() throws IOException;

    short readI16LE() throws IOException;
    int readI32LE() throws IOException;
    long readI64LE() throws IOException;
    float readF32LE() throws IOException;
    double readF64LE() throws IOException;

    short readI16BE() throws IOException;
    int readI32BE() throws IOException;
    long readI64BE() throws IOException;
    float readF32BE() throws IOException;
    double readF64BE() throws IOException;

    String readString(int bytes, Charset charset) throws IOException;

    void skip(int bytes) throws IOException;

    void seek(long pos) throws IOException;

    long pos();

    byte[] readBytes(long dataLength) throws IOException;
}
