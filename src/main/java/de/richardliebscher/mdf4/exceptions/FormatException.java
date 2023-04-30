/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.exceptions;

/**
 * Unable to parse MDF4 file, because it violates specification.
 */
public class FormatException extends ParseException {

  public FormatException(String message) {
    super(message);
  }

  public FormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public FormatException(Throwable cause) {
    super(cause);
  }
}
