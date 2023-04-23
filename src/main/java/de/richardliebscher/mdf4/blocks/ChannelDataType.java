/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;

public enum ChannelDataType {
    UINT_LE, UINT_BE,
    INT_LE, INT_BE,
    FLOAT_LE, FLOAT_BE,
    STRING_LATIN1, STRING_UTF8, STRING_UTF16LE, STRING_UTF16BE,
    BYTE_ARRAY,
    MIME_SAMPLE,
    MIME_STREAM,
    CANOPEN_DATE,
    CANOPEN_TIME,
    // 4.2.0
    COMPLEX_LE, COMPLEX_BE;

    private static final ChannelDataType[] VALUES = values();

    public static ChannelDataType parse(int value) throws FormatException {
        if (value >= 0 && value < VALUES.length) {
            return VALUES[value];
        } else {
            throw new FormatException("Unknown channel data type: " + value);
        }
    }
}
