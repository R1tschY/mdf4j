/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public final class ChannelSupport {

  private ChannelSupport() {
    // noop
  }

  public static void readFully(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
    while (buffer.remaining() > 0) {
      int bytes = channel.read(buffer);
      if (bytes == -1) {
        throw new EOFException();
      }
    }
  }
}
