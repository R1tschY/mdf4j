/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

module mdf4j {
  requires java.logging;
  requires static lombok;

  exports de.richardliebscher.mdf4;
  exports de.richardliebscher.mdf4.blocks;
  exports de.richardliebscher.mdf4.exceptions;
  exports de.richardliebscher.mdf4.extract;
  exports de.richardliebscher.mdf4.extract.de;
  exports de.richardliebscher.mdf4.io;
}