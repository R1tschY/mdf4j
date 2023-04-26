/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Unsigned {
    private static final long LONG_UNSIGNED_MASK = 0x7fffffffffffffffL;

    public static double doubleValue(long value) {
        var doubleValue = (double) (value & LONG_UNSIGNED_MASK);
        if (value < 0) {
            return doubleValue + 0x1.0p63;
        } else {
            return doubleValue;
        }
    }

    public static BigInteger bigIntegerValue(long value) {
        var bigIntegerValue = BigInteger.valueOf(value & LONG_UNSIGNED_MASK);
        if (value < 0) {
            return bigIntegerValue.setBit(Long.SIZE - 1);
        } else {
            return bigIntegerValue;
        }
    }
}
