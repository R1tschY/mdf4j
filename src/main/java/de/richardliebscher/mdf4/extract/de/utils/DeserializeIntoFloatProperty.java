/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;

/**
 * Deserialize into built-in 32-bit floating point property.
 *
 * @param <T> Type containing property
 */
public final class DeserializeIntoFloatProperty<T> implements DeserializeInto<T> {

  private final WriteFloatProperty<T> write;

  /**
   * Constructor.
   *
   * @param write Property writer
   */
  public DeserializeIntoFloatProperty(WriteFloatProperty<T> write) {
    this.write = write;
  }

  @Override
  public void deserializeInto(Deserializer deserializer, T object) throws IOException {
    deserializer.deserialize_value(new Visitor<Void, T>() {
      @Override
      public String expecting() {
        return "valid 32-bit floating point value";
      }

      @Override
      public Void visitF32(float value, T param) {
        write.writeFloat(param, value);
        return null;
      }
    }, object);
  }
}
