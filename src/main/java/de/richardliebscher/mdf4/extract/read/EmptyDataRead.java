/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.Data;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public class EmptyDataRead<T extends Data<T>> implements DataRead<T> {

  private boolean closed;

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }
    return -1;
  }

  @Override
  public boolean isOpen() {
    return !closed;
  }

  @Override
  public void close() throws IOException {
    closed = true;
  }
}
