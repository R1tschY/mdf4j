/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata.properties;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import lombok.ToString;

@ToString
@XmlAccessorType(XmlAccessType.FIELD)
final class Element implements PrimitiveValue, Property {
  @XmlAttribute(name = "ci")
  public int changeIndex = 0;

  @XmlAttribute(name = "name", required = true)
  public String name;

  @XmlAttribute(name = "desc")
  public String description;

  @XmlAttribute(name = "unit")
  public String unit;

  @XmlAttribute(name = "unit_ref")
  public String unitRef;

  @XmlAttribute(name = "type")
  public Type type = Type.STRING;

  @XmlAttribute(name = "ro")
  public boolean readOnly = false;

  @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
  public String language;

  @XmlValue
  public String value;

  @Override
  public String unit() {
    return unit;
  }

  @Override
  public void unit(String value) {
    this.unit = value;
  }

  @Override
  public String unitRef() {
    return unitRef;
  }

  @Override
  public void unitRef(String value) {
    this.unitRef = value;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public void type(Type value) {
    this.type = value;
  }

  @Override
  public boolean readOnly() {
    return readOnly;
  }

  @Override
  public void readOnly(boolean value) {
    this.readOnly = value;
  }

  @Override
  public String language() {
    return language;
  }

  @Override
  public void language(String value) {
    this.language = value;
  }

  @Override
  public String rawValue() {
    return value;
  }

  @Override
  public void rawValue(String value) {
    this.value = value;
  }

  @Override
  public Object javaValue() {
    return type.parse(value);
  }

  @Override
  public Class<?> javaType() {
    switch (type) {
      case STRING:
        return String.class;
      case DECIMAL:
        return BigDecimal.class;
      case INTEGER:
        return BigInteger.class;
      case FLOAT:
        return Float.class;
      case BOOLEAN:
        return Boolean.class;
      case DATE:
      case DATE_TIME:
        return OffsetDateTime.class;
      case TIME:
        return OffsetTime.class;
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
    return visitor.visitElement(this);
  }

  @Override
  public String getName() {
    return name;
  }
}
