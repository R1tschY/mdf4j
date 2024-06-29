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
 * Deserialize into nullable string property.
 *
 * @param <T> Type containing property
 */
public final class DeserializeIntoStringProperty<T> implements DeserializeInto<T> {

  private final WriteProperty<T, String> write;

  /**
   * Constructor.
   *
   * @param write Property writer
   */
  public DeserializeIntoStringProperty(WriteProperty<T, String> write) {
    this.write = write;
  }

  @Override
  public void deserializeInto(Deserializer deserializer, T object) throws IOException {
    deserializer.deserialize_value(new Visitor<Void, T>() {
      @Override
      public String expecting() {
        return "string";
      }

      @Override
      public Void visitInvalid(T param) {
        write.write(param, null);
        return null;
      }

      @Override
      public Void visitString(String value, T param) {
        write.write(param, value);
        return null;
      }
    }, object);
  }
}
