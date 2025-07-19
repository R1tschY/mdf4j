/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata.properties;

import java.util.List;
import java.util.Map;

public interface PropertyValue {

  Object javaValue();

  <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E;

  interface Visitor<T, E extends Throwable> {

    T visitElement(PrimitiveValue value) throws E;

    T visitTree(Map<String, PropertyValue> values) throws E;

    T visitList(List<PropertyValue> values) throws E;

    T visitSimpleList(List<PrimitiveValue> values) throws E;
  }
}
