/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public enum ChannelConversionFlag implements BitFlag {

  PRECISION_VALID(0),
  PHYSICAL_VALUE_RANGE_VALID(1),
  STATUS_STRING(2);

  private final int bitNumber;

  ChannelConversionFlag(int bitNumber) {
    this.bitNumber = bitNumber;
  }

  @Override
  public int bitNumber() {
    return bitNumber;
  }
}
