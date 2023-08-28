/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public enum UnfinalizedFlag implements BitFlag {

  DIRTY_CGCA_CYCLE_COUNTERS(0),
  DIRTY_SR_CYCLE_COUNTERS(1),
  DIRTY_LAST_DT_LENGTH(2),
  DIRTY_LAST_RD_LENGTH(3),
  DIRTY_LAST_DL(4),
  DIRTY_VLSD_BYTE_LENGTHS(5),
  DIRTY_VLSD_OFFSET(6);

  private final int bitNumber;

  UnfinalizedFlag(int bitNumber) {
    this.bitNumber = bitNumber;
  }

  @Override
  public int bitNumber() {
    return bitNumber;
  }
}
