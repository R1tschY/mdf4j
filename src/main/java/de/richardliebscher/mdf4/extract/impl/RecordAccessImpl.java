/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeSeed;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.ValueRead;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

class RecordAccessImpl implements RecordAccess {

  private final List<ValueRead> channelReaders;
  private final RecordBuffer input;
  private int index = 0;
  private final int size;

  public RecordAccessImpl(final List<ValueRead> channelReaders, final RecordBuffer input) {
    this.channelReaders = channelReaders;
    this.input = input;
    this.size = channelReaders.size();
  }

  @Override
  public <S extends DeserializeSeed<T>, T> T nextElementSeed(Deserialize<T> deserialize, S seed)
      throws IOException {
    if (index >= size) {
      throw new NoSuchElementException();
    }

    final var ret = seed.deserialize(deserialize, new Deserializer() {
      @Override
      public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
        return channelReaders.get(index).read(input, visitor);
      }
    });
    index += 1;
    return ret;
  }

  @Override
  public int remaining() {
    return channelReaders.size() - index;
  }
}
