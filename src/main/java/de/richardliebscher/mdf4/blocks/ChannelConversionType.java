package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;

public enum ChannelConversionType {
    IDENTITY,
    LINEAR,
    RATIONAL,
    ALGEBRAIC,
    INTERPOLATED_VALUE_TABLE,
    VALUE_VALUE_TABLE,
    VALUE_RANGE_VALUE_TABLE,
    VALUE_TEXT_TABLE,
    VALUE_RANGE_TEXT_TABLE,
    TEXT_VALUE_TABLE,
    TEXT_TEXT_TABLE,
    BITFIELD_TEXT_TABLE;

    private static final ChannelConversionType[] VALUES = values();

    public static ChannelConversionType parse(int value) throws FormatException {
        if (value >= 0 && value < VALUES.length) {
            return VALUES[value];
        } else {
            throw new FormatException("Unknown channel conversion type: " + value);
        }
    }
}
