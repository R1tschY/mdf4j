/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */


package de.richardliebscher.mdf4.blocks;

public enum ChannelGroupFlag implements BitFlag {
  VLSD_CHANNEL_GROUP(0),
  BUS_EVENT_CHANNEL_GROUP(1),
  PLAIN_BUS_EVENT_CHANNEL_GROUP(2),
  REMOTE_MASTER(3),
  EVENT_SIGNAL_GROUP(4);

  private final int bitNumber;

  ChannelGroupFlag(int bitNumber) {
    this.bitNumber = bitNumber;
  }

  @Override
  public int bitNumber() {
    return bitNumber;
  }
}
