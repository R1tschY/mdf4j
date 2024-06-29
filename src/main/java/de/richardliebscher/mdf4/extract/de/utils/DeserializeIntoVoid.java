/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import java.io.IOException;

/**
 * Do not deserialize.
 *
 * @param <T> Type to do nothing with it
 */
public final class DeserializeIntoVoid<T> implements DeserializeInto<T> {
  @Override
  public void deserializeInto(Deserializer deserializer, T object) throws IOException {
    deserializer.ignore();
  }
}
