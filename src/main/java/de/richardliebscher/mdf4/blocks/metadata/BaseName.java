package de.richardliebscher.mdf4.blocks.metadata;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static de.richardliebscher.mdf4.blocks.metadata.Utils.defaultEmptyValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class BaseName {
  @XmlValue
  @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
  private String value;

  @XmlAttribute(name = "ci")
  private String ci = "0";

  @XmlAnyAttribute
  private Map<QName, String> otherAttributes;

  private BaseName() {
    otherAttributes = new HashMap<>();
  }

  public BaseName(String value, String ci, Map<QName, String> otherAttributes) {
    this.value = value;
    this.ci = ci;
    this.otherAttributes = defaultEmptyValue(otherAttributes);
  }

  public String value() {
    return value;
  }

  public String ci() {
    return ci;
  }

  public Map<QName, String> otherAttributes() {
    return Collections.unmodifiableMap(otherAttributes);
  }

  @Override
  public String toString() {
    return '\'' + value + '\'' + "{ci='" + ci + "', otherAttributes=" + otherAttributes + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaseName baseName = (BaseName) o;
    return Objects.equals(value, baseName.value)
            && Objects.equals(ci, baseName.ci)
            && Objects.equals(otherAttributes, baseName.otherAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, ci, otherAttributes);
  }
}
