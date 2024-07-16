/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.extract.de.Unsigned;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum HeaderFlag implements BitFlag {
  START_ANGLE_VALID(0),
  START_DISTANCE_VALID(1);

  private final @Unsigned int bitNumber;

  @Override
  public @Unsigned int bitNumber() {
    return bitNumber;
  }
}
