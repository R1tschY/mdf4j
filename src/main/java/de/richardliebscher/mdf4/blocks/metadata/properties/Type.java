/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata.properties;

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.XmlEnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Type {
  @XmlEnumValue("string") STRING("string"),
  @XmlEnumValue("decimal") DECIMAL("decimal"),
  @XmlEnumValue("integer") INTEGER("integer"),
  @XmlEnumValue("float") FLOAT("float"),
  @XmlEnumValue("boolean") BOOLEAN("boolean"),
  @XmlEnumValue("date") DATE("date"),
  @XmlEnumValue("time") TIME("time"),
  @XmlEnumValue("dateTime") DATE_TIME("dateTime");

  private final String name;

  public Object parse(String value) {
    switch (this) {
      case STRING:
        return value;
      case DECIMAL:
        return DatatypeConverter.parseDecimal(value);
      case INTEGER:
        return DatatypeConverter.parseInteger(value);
      case FLOAT:
        return DatatypeConverter.parseFloat(value);
      case BOOLEAN:
        return DatatypeConverter.parseBoolean(value);
      case DATE:
        return DatatypeConverter.parseDate(value).toInstant();
      case TIME:
        return DatatypeConverter.parseTime(value);
      case DATE_TIME:
        return DatatypeConverter.parseDateTime(value);
      default:
        throw new IllegalStateException();
    }
  }
}
