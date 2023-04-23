/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public class DataListFlags extends FlagsBase<DataListFlags> {
    public static final DataListFlags EQUAL_LENGTH = ofBit(0);
    public static final DataListFlags TIME_VALUES = ofBit(1);
    public static final DataListFlags ANGLE_VALUES = ofBit(2);
    public static final DataListFlags DISTANCE_VALUES = ofBit(3);

    public static DataListFlags of(int flags) {
        return new DataListFlags(flags);
    }

    private static DataListFlags ofBit(int bit) {
        return new DataListFlags(1 << bit);
    }

    private DataListFlags(int value) {
        super(value);
    }

    @Override
    protected DataListFlags create(int a) {
        return new DataListFlags(a);
    }
}
