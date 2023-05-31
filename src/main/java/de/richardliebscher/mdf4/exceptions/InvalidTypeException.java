/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.exceptions;

import de.richardliebscher.mdf4.extract.de.Expected;

/**
 * Got value of unexpected type.
 */
public class InvalidTypeException extends DeserializationException {

  public InvalidTypeException(String unexpected, Expected expected) {
    super("Invalid type: " + unexpected + ", expected " + expected.expecting());
  }
}
