/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.extract.de.Unsigned;

public enum TimeFlag implements BitFlag {
  LOCAL_TIME(0),
  TIME_OFFSET_VALID(1);

  private final int bitNumber;

  TimeFlag(int bitNumber) {
    this.bitNumber = bitNumber;
  }

  @Override
  public @Unsigned int bitNumber() {
    return bitNumber;
  }
}
