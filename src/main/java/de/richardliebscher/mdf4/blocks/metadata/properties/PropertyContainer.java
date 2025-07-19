/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks.metadata.properties;


import de.richardliebscher.mdf4.blocks.metadata.ElementBase;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Container for properties.
 */
@XmlJavaTypeAdapter(PropertyContainer.Adapter.class)
public class PropertyContainer extends LinkedHashMap<String, PropertyValue> implements
    PropertyValue {

  @XmlAttribute(name = "ci")
  public int creatorIndex = 0;

  public PropertyContainer() {
  }

  public PropertyContainer(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public Map<String, Object> javaValue() {
    final var result = new HashMap<String, Object>(size());
    forEach((key, value) -> result.put(key, value.javaValue()));
    return result;
  }

  @Override
  public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
    return visitor.visitTree(this);
  }

  static class Adapter extends XmlAdapter<XmlValue, PropertyContainer> {

    @Override
    public PropertyContainer unmarshal(XmlValue v) {
      final var result = new PropertyContainer(v.properties.size());
      for (final var property : v.properties) {
        result.put(property.getName(), property);
      }
      return result;
    }

    @Override
    public XmlValue marshal(PropertyContainer v) {
      List<Property> properties = new ArrayList<>(v.size());
      for (final var prop : v.entrySet()) {
        properties.add((Property) prop.getValue());
      }
      return new XmlValue(properties);
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @XmlAccessorType(XmlAccessType.FIELD)
  static class XmlValue extends ElementBase {

    @XmlElements({
        @XmlElement(name = "e", type = Element.class),
        @XmlElement(name = "tree", type = NamedContainer.class),
        @XmlElement(name = "list", type = ElementList.class),
        @XmlElement(name = "elist", type = SimpleElementList.class),
    })
    public List<Property> properties;
  }
}
