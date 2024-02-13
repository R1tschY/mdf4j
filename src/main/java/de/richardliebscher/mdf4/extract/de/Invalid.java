/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import de.richardliebscher.mdf4.blocks.ChannelBlock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * Type for an invalid value.
 *
 * @see ChannelBlock#getInvalidationBit()
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Invalid {

  private static final Invalid INSTANCE = new Invalid();

  /**
   * Get representation for an invalid value.
   *
   * @return The invalid singleton
   */
  public static Invalid get() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "N/A";
  }
}
