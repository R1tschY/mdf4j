/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata.properties;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
final class ElementList extends NamedElement implements PropertyValue {

  @XmlElement(name = "li")
  public List<PropertyContainer> items;

  @Override
  public Object javaValue() {
    return null;
  }

  @Override
  public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
    return visitor.visitList(new ArrayList<>(items));
  }
}
