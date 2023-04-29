/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public class CustomFlags extends FlagsBase<CustomFlags> {
    public static CustomFlags of(int flags) {
        return new CustomFlags(flags);
    }

    public static CustomFlags ofBit(int bit) {
        return new CustomFlags(1 << bit);
    }

    private CustomFlags(int value) {
        super(value);
    }

    @Override
    protected CustomFlags create(int a) {
        return new CustomFlags(a);
    }
}
