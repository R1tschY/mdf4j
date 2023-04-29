/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public final class ChannelGroupFlags extends FlagsBase<ChannelGroupFlags> {

  public static final ChannelGroupFlags VLSD_CHANNEL_GROUP = ofBit(0);
  public static final ChannelGroupFlags BUS_EVENT_CHANNEL_GROUP = ofBit(1);
  public static final ChannelGroupFlags PLAIN_BUS_EVENT_CHANNEL_GROUP = ofBit(2);
  public static final ChannelGroupFlags REMOTE_MASTER = ofBit(3);
  public static final ChannelGroupFlags EVENT_SIGNAL_GROUP = ofBit(4);

  public static ChannelGroupFlags of(int flags) {
    return new ChannelGroupFlags(flags);
  }

  private static ChannelGroupFlags ofBit(int bit) {
    return new ChannelGroupFlags(1 << bit);
  }

  private ChannelGroupFlags(int value) {
    super(value);
  }

  @Override
  protected ChannelGroupFlags create(int a) {
    return new ChannelGroupFlags(a);
  }
}
