/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import de.richardliebscher.mdf4.extract.de.UnsignedShort;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;

/**
 * Deserialize into built-in 32-bit integer property.
 *
 * @param <T> Type containing property
 */
public final class DeserializeIntoIntProperty<T> implements DeserializeInto<T> {

  private final WriteIntProperty<T> write;

  /**
   * Constructor.
   *
   * @param write Property writer
   */
  public DeserializeIntoIntProperty(WriteIntProperty<T> write) {
    this.write = write;
  }

  @Override
  public void deserializeInto(Deserializer deserializer, T object) throws IOException {
    deserializer.deserialize_value(new Visitor<Void, T>() {
      @Override
      public String expecting() {
        return "valid 32-bit integer";
      }

      @Override
      public Void visitI32(int value, T param) {
        write.writeInt(param, value);
        return null;
      }

      @Override
      public Void visitU16(short value, T param) {
        return visitI32(UnsignedShort.toInt(value), param);
      }

      @Override
      public Void visitU8(byte value, T param) {
        return visitI32(UnsignedByte.toInt(value), param);
      }
    }, object);
  }
}
