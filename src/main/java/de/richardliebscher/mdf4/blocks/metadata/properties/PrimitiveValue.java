/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata.properties;

public interface PrimitiveValue extends PropertyValue {

  String unit();

  void unit(String value);

  String unitRef();

  void unitRef(String value);

  Type type();

  void type(Type value);

  boolean readOnly();

  void readOnly(boolean value);

  String language();

  void language(String value);

  String rawValue();

  void rawValue(String value);

  Class<?> javaType();

}
