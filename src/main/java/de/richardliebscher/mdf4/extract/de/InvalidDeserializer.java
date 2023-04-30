/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

/**
 * Deserializer for an invalid value.
 */
public class InvalidDeserializer implements Deserializer {

  @Override
  public <R> R deserialize_value(Visitor<R> visitor) {
    return visitor.visitInvalid();
  }
}
