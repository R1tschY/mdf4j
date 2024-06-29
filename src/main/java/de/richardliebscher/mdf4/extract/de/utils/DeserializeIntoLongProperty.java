/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;

/**
 * Deserialize into built-in 64-bit integer property.
 *
 * @param <T> Type containing property
 */
public final class DeserializeIntoLongProperty<T> implements DeserializeInto<T> {

  private final WriteLongProperty<T> write;

  /**
   * Constructor.
   *
   * @param write Property writer
   */
  public DeserializeIntoLongProperty(WriteLongProperty<T> write) {
    this.write = write;
  }

  @Override
  public void deserializeInto(Deserializer deserializer, T object) throws IOException {
    deserializer.deserialize_value(new Visitor<Void, T>() {
      @Override
      public String expecting() {
        return "valid 64-bit integer";
      }

      @Override
      public Void visitI64(long value, T param) {
        write.writeLong(param, value);
        return null;
      }

      @Override
      public Void visitU32(int value, T param) {
        return visitI64(UnsignedInteger.toLong(value), param);
      }
    }, object);
  }
}
