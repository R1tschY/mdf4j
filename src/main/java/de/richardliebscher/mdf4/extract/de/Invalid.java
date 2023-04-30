/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import de.richardliebscher.mdf4.blocks.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * Type for an invalid value.
 *
 * @see Channel#getInvalidationBit()
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Invalid {

  private static final int HASH = Invalid.class.hashCode();

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
  public boolean equals(Object obj) {
    return obj instanceof Invalid;
  }

  @Override
  public int hashCode() {
    return HASH;
  }

  @Override
  public String toString() {
    return "N/A";
  }
}
