/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Ignored value singleton.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IgnoreAny implements Deserialize<IgnoreAny> {
  private static final IgnoreAny INSTANCE = new IgnoreAny();

  /**
   * Get representation for an ignored value.
   *
   * @return The ignored value singleton
   */
  public static IgnoreAny get() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "IgnoreAny";
  }

  @Override
  public IgnoreAny deserialize(Deserializer deserializer) throws IOException {
    deserializer.ignore();
    return INSTANCE;
  }
}
