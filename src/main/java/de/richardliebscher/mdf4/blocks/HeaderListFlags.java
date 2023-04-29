/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public class HeaderListFlags extends FlagsBase<HeaderListFlags> {
    public static final HeaderListFlags EQUAL_LENGTH = ofBit(0);
    public static final HeaderListFlags TIME_VALUES = ofBit(1);
    public static final HeaderListFlags ANGLE_VALUES = ofBit(2);
    public static final HeaderListFlags DISTANCE_VALUES = ofBit(3);
    public static final HeaderListFlags ALL =
            EQUAL_LENGTH.merge(TIME_VALUES.merge(ANGLE_VALUES.merge(DISTANCE_VALUES)));

    public static HeaderListFlags of(int flags) {
        return new HeaderListFlags(flags);
    }

    private static HeaderListFlags ofBit(int bit) {
        return new HeaderListFlags(1 << bit);
    }

    private HeaderListFlags(int value) {
        super(value);
    }

    @Override
    protected HeaderListFlags create(int a) {
        return new HeaderListFlags(a);
    }

    public boolean hasUnknown() {
        return ((~ALL.value) & value) != 0;
    }
}
