/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.extract.de.Unsigned;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TimeClass implements KnownValue {
  LOCAL_PC(0),
  EXTERNAL_TIME_SOURCE(10),
  EXTERNAL_ABSOLUTE_SYNCHRONIZED_TIME(16);

  private final @Unsigned int intValue;

  @Override
  public @Unsigned int intValue() {
    return intValue;
  }
}
