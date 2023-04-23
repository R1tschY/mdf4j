/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Unsigned {
    private static final long LONG_UNSIGNED_MASK = 0x7fffffffffffffffL;

    public static double doubleValue(long value) {
        double doubleValue = (double) (value & LONG_UNSIGNED_MASK);
        if (value < 0) {
            doubleValue += 0x1.0p63;
        }
        return doubleValue;
    }
}
