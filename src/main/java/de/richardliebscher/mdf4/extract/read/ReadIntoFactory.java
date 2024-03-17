/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ReadIntoFactory<T> extends Serializable {

  ReadInto<T> build(ByteInput input, Scope scope) throws IOException;

  static <T> List<ReadInto<T>> buildAll(Collection<ReadIntoFactory<T>> factories, ByteInput input,
      Scope scope) throws IOException {
    final var result = new ArrayList<ReadInto<T>>(factories.size());
    for (final var channelReaderFactory : factories) {
      result.add(channelReaderFactory.build(input, scope));
    }
    return result;
  }
}
