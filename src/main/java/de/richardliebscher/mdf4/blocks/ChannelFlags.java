package de.richardliebscher.mdf4.blocks;

public final class ChannelFlags extends FlagsBase<ChannelFlags> {
    public static final ChannelFlags ALL_VALUES_INVALID = ofBit(0);
    public static final ChannelFlags INVALIDATION_BIT_VALID = ofBit(1);
    public static final ChannelFlags PRECISION_VALID = ofBit(2);
    public static final ChannelFlags VALUE_RANGE_VALID = ofBit(3);
    public static final ChannelFlags LIMIT_RANGE_VALID = ofBit(4);
    public static final ChannelFlags EXTENDED_LIMIT_RANGE_VALID = ofBit(5);
    public static final ChannelFlags DISCRETE_VALUE = ofBit(6);
    public static final ChannelFlags CALIBRATION = ofBit(7);
    public static final ChannelFlags CALCULATED = ofBit(8);
    public static final ChannelFlags VIRTUAL = ofBit(9);
    public static final ChannelFlags BUS_EVENT = ofBit(10);
    public static final ChannelFlags STRICTLY_MONOTONOUS = ofBit(11);
    public static final ChannelFlags DEFAULT_X_AXIS = ofBit(12);
    public static final ChannelFlags EVENT_SIGNAL = ofBit(13);
    public static final ChannelFlags VLSD_DATA_STREAM = ofBit(14);

    public static ChannelFlags of(int flags) {
        return new ChannelFlags(flags);
    }

    private static ChannelFlags ofBit(int bit) {
        return new ChannelFlags(1 << bit);
    }

    private ChannelFlags(int value) {
        super(value);
    }

    @Override
    protected ChannelFlags create(int a) {
        return new ChannelFlags(a);
    }
}
