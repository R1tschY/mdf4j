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

    public static long longValue(long value) {
        return value & LONG_UNSIGNED_MASK;
    }

    public static float floatValue(long value) {
        if (value < 0) {
            return ((value >>> 1) | (value & 1)) * 2.0f;
        } else {
            return value;
        }
    }

    public static double doubleValue(long value) {
        if (value < 0) {
            return ((value >>> 1) | (value & 1)) * 2.0;
        } else {
            return value;
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
