/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;

public enum ChannelType {
    FIXED_LENGTH_DATA_CHANNEL,
    VARIABLE_LENGTH_DATA_CHANNEL,
    MASTER_CHANNEL,
    VIRTUAL_MASTER_CHANNEL,
    SYNCHRONIZATION_CHANNEL,
    MAXIMUM_LENGTH_CHANNEL,
    VIRTUAL_DATA_CHANNEL;

    private static final ChannelType[] VALUES = values();

    public static ChannelType parse(int value) throws FormatException {
        if (value >= 0 && value < VALUES.length) {
            return VALUES[value];
        } else {
            throw new FormatException("Unknown channel type: " + value);
        }
    }
}
