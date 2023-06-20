/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Half;
import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.UnsignedShort;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;
import java.io.Serializable;


class TestReprDeserialize implements Deserialize<String>, Serializable {

  @Override
  public String deserialize(Deserializer deserializer) throws IOException {
    return deserializer.deserialize_value(new Visitor<>() {
      @Override
      public String expecting() {
        return "any value";
      }

      @Override
      public String visitU8(byte value) {
        return UnsignedByte.toString(value) + "hhu";
      }

      @Override
      public String visitU16(short value) {
        return UnsignedShort.toString(value) + "hu";
      }

      @Override
      public String visitU32(int value) {
        return UnsignedInteger.toString(value) + "u";
      }

      @Override
      public String visitU64(long value) {
        return UnsignedLong.toString(value) + "lu";
      }

      @Override
      public String visitI8(byte value) {
        return value + "hh";
      }

      @Override
      public String visitI16(short value) {
        return value + "h";
      }

      @Override
      public String visitI32(int value) {
        return Integer.toString(value);
      }

      @Override
      public String visitI64(long value) {
        return value + "l";
      }

      @Override
      public String visitF16(short value) {
        return Half.toString(value) + "hf";
      }

      @Override
      public String visitF32(float value) {
        return value + "f";
      }

      @Override
      public String visitF64(double value) {
        return value + "d";
      }

      @Override
      public String visitInvalid() {
        return "";
      }
    });
  }
}

