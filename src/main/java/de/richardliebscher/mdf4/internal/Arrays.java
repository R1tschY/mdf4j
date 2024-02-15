/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import java.lang.reflect.Array;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Arrays {

  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(Class<T> clazz, int length) {
    return (T[]) Array.newInstance(clazz, length);
  }

  public static int indexOf(byte[] arr, byte elem) {
    return indexOf(arr, 0, arr.length, elem);
  }

  public static int indexOf(byte[] arr, int offset, int length, byte elem) {
    checkRange(arr, offset, length);

    for (int i = offset; i < offset + length; i++) {
      if (arr[i] == elem) {
        return i;
      }
    }
    return -1;
  }

  private static void checkRange(byte[] arr, int offset, int length) {
    if (length < 0) {
      throw new IllegalArgumentException("length should not be negative");
    }
    if (offset < 0 || offset > arr.length) {
      throw new ArrayIndexOutOfBoundsException(offset);
    }
    if (offset + length > arr.length) {
      throw new ArrayIndexOutOfBoundsException(offset + length - 1);
    }
  }
}
