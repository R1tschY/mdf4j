/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.datatypes;

import java.util.Collections;
import java.util.List;

/**
 * Structure composition.
 */
public final class StructType implements DataType {

  private final List<StructField> fields;

  public StructType(List<StructField> fields) {
    this.fields = Collections.unmodifiableList(fields);
  }

  public List<StructField> fields() {
    return fields;
  }

  @Override
  public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
    return visitor.visit(this);
  }
}
