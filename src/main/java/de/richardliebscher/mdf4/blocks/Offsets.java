/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Offsets implements Serializable {

  public abstract <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E;

  public abstract long size();

  public abstract long last();

  public abstract int indexOfPosition(long position);

  public abstract long get(int index);

  public static final class EqualLength extends Offsets {

    private final int number;
    private final long length;

    public EqualLength(long number, long length) throws NotImplementedFeatureException {
      if (number != (int) number) {
        throw new NotImplementedFeatureException("Too many DT blocks: " + number);
      }

      this.number = (int) number;
      this.length = length;
    }

    @Override
    public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
      return visitor.visitLength(number, length);
    }

    @Override
    public long size() {
      return number;
    }

    @Override
    public long last() {
      return number > 0 ? (number - 1) * length : 0;
    }

    @Override
    public int indexOfPosition(long position) {
      if (position < 0) {
        throw new IllegalArgumentException();
      }

      return Math.min(Math.toIntExact(position / length), number - 1);
    }

    @Override
    public long get(int index) {
      if (index < 0) {
        throw new IllegalArgumentException();
      }
      if (index >= number) {
        throw new IndexOutOfBoundsException(index);
      }

      return index * length;
    }
  }

  public static final class Values extends Offsets {

    private final long[] offsets;

    public Values(long[] offsets) throws FormatException {
      if (offsets.length > 0 && offsets[0] != 0) {
        throw new FormatException("First offset must be 0");
      }

      this.offsets = offsets;
    }

    @Override
    public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
      return visitor.visitOffsets(offsets);
    }

    @Override
    public long size() {
      return offsets.length;
    }

    @Override
    public long last() {
      return offsets.length > 0 ? offsets[offsets.length - 1] : 0;
    }

    @Override
    public int indexOfPosition(long position) {
      for (int i = 1; i < offsets.length; i++) {
        if (offsets[i] > position) {
          return i - 1;
        }
      }

      return offsets.length - 1;
    }

    @Override
    public long get(int index) {
      if (index < 0) {
        throw new IllegalArgumentException();
      }
      if (index >= offsets.length) {
        throw new IndexOutOfBoundsException(index);
      }

      return offsets[index];
    }
  }

  public interface Visitor<R, E extends Throwable> {

    R visitLength(int number, long length) throws E;

    R visitOffsets(long[] offsets) throws E;
  }
}
