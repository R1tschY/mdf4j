/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class LengthOrOffsets {

  public abstract <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E;

  @RequiredArgsConstructor
  public static final class Length extends LengthOrOffsets {

    private final long length;

    @Override
    public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
      return visitor.visitLength(length);
    }
  }

  @RequiredArgsConstructor
  public static final class Offsets extends LengthOrOffsets {

    private final long[] offsets;

    @Override
    public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
      return visitor.visitOffsets(offsets);
    }
  }

  public interface Visitor<R, E extends Throwable> {

    R visitLength(long length) throws E;

    R visitOffsets(long[] offsets) throws E;
  }
}
