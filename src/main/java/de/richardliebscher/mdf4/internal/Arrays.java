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
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == elem) {
        return i;
      }
    }
    return -1;
  }
}
