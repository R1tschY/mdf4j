/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.datatypes;

/**
 * Field of a structure composition.
 */
public class StructField {
  private final String name;
  private final DataType dataType;

  public StructField(String name, DataType dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  public String name() {
    return name;
  }

  public DataType dataType() {
    return dataType;
  }
}
