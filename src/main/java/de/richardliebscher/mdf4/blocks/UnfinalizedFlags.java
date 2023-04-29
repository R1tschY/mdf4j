/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public class UnfinalizedFlags extends FlagsBase<UnfinalizedFlags> {
    public static final UnfinalizedFlags DIRTY_CGCA_CYCLE_COUNTERS = ofBit(0);
    public static final UnfinalizedFlags DIRTY_SR_CYCLE_COUNTERS = ofBit(1);
    public static final UnfinalizedFlags DIRTY_LAST_DT_LENGTH = ofBit(2);
    public static final UnfinalizedFlags DIRTY_LAST_RD_LENGTH = ofBit(3);
    public static final UnfinalizedFlags DIRTY_LAST_DL = ofBit(4);
    public static final UnfinalizedFlags DIRTY_VLSD_BYTE_LENGTHS = ofBit(5);
    public static final UnfinalizedFlags DIRTY_VLSD_OFFSET = ofBit(6);

    public static UnfinalizedFlags of(int flags) {
        return new UnfinalizedFlags(flags);
    }

    private static UnfinalizedFlags ofBit(int bit) {
        return new UnfinalizedFlags(1 << bit);
    }

    private UnfinalizedFlags(int value) {
        super(value);
    }

    @Override
    protected UnfinalizedFlags create(int a) {
        return new UnfinalizedFlags(a);
    }
}
