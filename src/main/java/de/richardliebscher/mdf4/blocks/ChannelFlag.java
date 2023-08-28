/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public enum ChannelFlag implements BitFlag {

  ALL_VALUES_INVALID(0),
  INVALIDATION_BIT_VALID(1),
  PRECISION_VALID(2),
  VALUE_RANGE_VALID(3),
  LIMIT_RANGE_VALID(4),
  EXTENDED_LIMIT_RANGE_VALID(5),
  DISCRETE_VALUE(6),
  CALIBRATION(7),
  CALCULATED(8),
  VIRTUAL(9),
  BUS_EVENT(10),
  STRICTLY_MONOTONOUS(11),
  DEFAULT_X_AXIS(12),
  EVENT_SIGNAL(13),
  VLSD_DATA_STREAM(14);

  private final int bitNumber;

  ChannelFlag(int bitNumber) {
    this.bitNumber = bitNumber;
  }

  @Override
  public int bitNumber() {
    return bitNumber;
  }
}
