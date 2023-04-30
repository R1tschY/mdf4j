/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.exceptions;

import de.richardliebscher.mdf4.MdfFormatVersion;
import lombok.Getter;

/**
 * MDF4 file has supported major version or has newer version that forbids reading an HL-Block.
 */
public class UnsupportedVersionException extends ParseException {

  @Getter
  private final MdfFormatVersion unsupportedVersion;

  public UnsupportedVersionException(String message) {
    super(message);
    this.unsupportedVersion = null;
  }

  public UnsupportedVersionException(MdfFormatVersion unsupportedVersion) {
    super("Unsupported major version: " + unsupportedVersion.getMajor());
    this.unsupportedVersion = unsupportedVersion;
  }
}
