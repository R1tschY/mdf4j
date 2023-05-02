/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NoValueRead implements ValueRead {

  private final Deserializer deserializer;

  @Override
  public <T> T read(RecordBuffer ignore, Visitor<T> visitor) throws IOException {
    return deserializer.deserialize_value(visitor);
  }
}
