package de.richardliebscher.mdf4.blocks.metadata;

import jakarta.xml.bind.annotation.*;

import javax.xml.namespace.QName;
import java.util.*;

import static de.richardliebscher.mdf4.blocks.metadata.Utils.defaultEmptyValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Extension {
  @XmlAnyElement(lax = true)
  private List<Object> any = new ArrayList<>();

  @XmlAttribute(name = "ci")
  private String ci = "0";

  @XmlAnyAttribute
  private Map<QName, String> otherAttributes = new HashMap<>();

  private Extension() {
  }

  public Extension(List<Object> any, String ci, Map<QName, String> otherAttributes) {
    this.any = defaultEmptyValue(any);
    this.ci = ci;
    this.otherAttributes = defaultEmptyValue(otherAttributes);
  }

  public List<Object> any() {
    return any;
  }

  public String ci() {
    return ci;
  }

  public Map<QName, String> otherAttributes() {
    return otherAttributes;
  }

  @Override
  public String toString() {
    return "Extension{" +
            "any=" + any +
            ", ci='" + ci + '\'' +
            ", otherAttributes=" + otherAttributes +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Extension extension = (Extension) o;
    return Objects.equals(any, extension.any)
            && Objects.equals(ci, extension.ci)
            && Objects.equals(otherAttributes, extension.otherAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(any, ci, otherAttributes);
  }
}
