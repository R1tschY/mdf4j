/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;

public class SizeVisitor implements Visitor<Void> {
  // See https://github.com/openjdk/jdk/blob/a0e5e16afbd19f6396f0af2cba954225a357eca8/src/java.base/share/classes/jdk/internal/util/ArraysSupport.java#L692
  public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

  private int value;

  public int getValue() {
    return value;
  }

  @Override
  public String expecting() {
    return "size";
  }

  @Override
  public Void visitU64(long value) throws IOException {
    if (value < 0 || value > MAX_ARRAY_LENGTH) {
      throw new NotImplementedFeatureException("Not big value for size channel: "
          + UnsignedLong.toString(value));
    }

    this.value = (int) value;
    return null;
  }

  @Override
  public Void visitI32(int value) throws IOException {
    if (value < 0) {
      throw new FormatException("Invalid value for size channel: " + value);
    } else if (value > MAX_ARRAY_LENGTH) {
      throw new NotImplementedFeatureException("Not big value for size channel: " + value);
    }

    this.value = value;
    return null;
  }

  @Override
  public Void visitI64(long value) throws IOException {
    if (value < 0) {
      throw new FormatException("Invalid value for size channel: " + value);
    } else if (value > MAX_ARRAY_LENGTH) {
      throw new NotImplementedFeatureException("Not big value for size channel: " + value);
    }

    this.value = (int) value;
    return null;
  }
}
