/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.internal.LongCell;
import java.io.IOException;

public final class UnsignedLongVisitor implements Visitor<Void, LongCell> {
  public static final UnsignedLongVisitor INSTANCE = new UnsignedLongVisitor();

  private UnsignedLongVisitor() {
  }

  @Override
  public String expecting() {
    return "unsigned long integer";
  }

  @Override
  public Void visitU32(int value, LongCell target) {
    target.set(UnsignedInteger.toLong(value));
    return null;
  }

  @Override
  public Void visitU64(long value, LongCell target) throws IOException {
    if (value < 0) {
      throw new NotImplementedFeatureException("Not big value: " + UnsignedLong.toString(value));
    }

    target.set(value);
    return null;
  }
}
