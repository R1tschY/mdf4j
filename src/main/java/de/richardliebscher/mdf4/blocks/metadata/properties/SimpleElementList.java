/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata.properties;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.FIELD)
final class SimpleElementList extends NamedElement implements PropertyValue {

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

  @XmlElement(name = "eli")
  public List<SimpleElementListElement> items;

  @Override
  public List<Object> javaValue() {
    final var result = new ArrayList<>(items.size());
    items.forEach(value -> result.add(type.parse(value.value)));
    return result;
  }

  @Override
  public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
    return visitor.visitSimpleList(items.stream()
        .map(item -> {
          final var element = new Element();
          element.unit = this.unit;
          element.unitRef = this.unitRef;
          element.type = this.type;
          element.readOnly = this.readOnly;
          element.language = this.language;
          element.value = item.value;
          return element;
        }).collect(Collectors.toList()));
  }
}
