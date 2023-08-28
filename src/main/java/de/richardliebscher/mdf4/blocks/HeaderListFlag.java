/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public enum HeaderListFlag implements BitFlag {

  EQUAL_LENGTH(0),
  TIME_VALUES(1),
  ANGLE_VALUES(2),
  DISTANCE_VALUES(3);

  private final int bitNumber;

  HeaderListFlag(int bitNumber) {
    this.bitNumber = bitNumber;
  }

  @Override
  public int bitNumber() {
    return bitNumber;
  }
}
