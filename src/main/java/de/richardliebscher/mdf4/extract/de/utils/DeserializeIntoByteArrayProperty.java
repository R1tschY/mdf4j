/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de.utils;

import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;
import java.util.Arrays;

/**
 * Deserialize into nullable string property.
 *
 * @param <T> Type containing property
 */
public final class DeserializeIntoByteArrayProperty<T> implements DeserializeInto<T> {

  private final WriteProperty<T, byte[]> write;

  /**
   * Constructor.
   *
   * @param write Property writer
   */
  public DeserializeIntoByteArrayProperty(WriteProperty<T, byte[]> write) {
    this.write = write;
  }

  @Override
  public void deserializeInto(Deserializer deserializer, T object) throws IOException {
    deserializer.deserialize_value(new Visitor<Void, T>() {
      @Override
      public String expecting() {
        return "byte array";
      }

      @Override
      public Void visitInvalid(T param) {
        write.write(param, null);
        return null;
      }

      @Override
      public Void visitByteArray(byte[] bytes, T param) {
        write.write(param, bytes);
        return null;
      }

      @Override
      public Void visitByteArray(byte[] bytes, int offset, int length, T param) {
        write.write(param, Arrays.copyOfRange(bytes, offset, offset + length));
        return null;
      }
    }, object);
  }
}
