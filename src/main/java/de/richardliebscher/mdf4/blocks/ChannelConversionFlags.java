/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public final class ChannelConversionFlags extends FlagsBase<ChannelConversionFlags> {

  public static final ChannelConversionFlags PRECISION_VALID = ofBit(0);
  public static final ChannelConversionFlags PHYSICAL_VALUE_RANGE_VALID = ofBit(1);
  public static final ChannelConversionFlags STATUS_STRING = ofBit(2);

  public static ChannelConversionFlags of(int flags) {
    return new ChannelConversionFlags(flags);
  }

  private static ChannelConversionFlags ofBit(int bit) {
    return new ChannelConversionFlags(1 << bit);
  }

  private ChannelConversionFlags(int value) {
    super(value);
  }

  @Override
  protected ChannelConversionFlags create(int a) {
    return new ChannelConversionFlags(a);
  }
}
