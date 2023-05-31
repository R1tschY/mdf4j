/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

/**
 * Deserializer for an invalid value.
 */
public class InvalidDeserializer implements Deserializer {

  @Override
  public <R> R deserialize_value(Visitor<R> visitor) throws IOException {
    return visitor.visitInvalid();
  }
}
