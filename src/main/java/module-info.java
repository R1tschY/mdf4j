/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

/**
 * Read MDF4 files in Java.
 *
 * @see de.richardliebscher.mdf4.Mdf4File
 */
module mdf4j {
  requires java.logging;
  requires java.xml;
  requires jakarta.xml.bind;
  requires static lombok;

  exports de.richardliebscher.mdf4;
  exports de.richardliebscher.mdf4.datatypes;
  exports de.richardliebscher.mdf4.blocks;
  exports de.richardliebscher.mdf4.exceptions;
  exports de.richardliebscher.mdf4.extract;
  exports de.richardliebscher.mdf4.extract.de;
  exports de.richardliebscher.mdf4.io;
}