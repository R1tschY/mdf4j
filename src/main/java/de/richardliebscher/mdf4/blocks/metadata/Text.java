package de.richardliebscher.mdf4.blocks.metadata;

import jakarta.xml.bind.annotation.*;

import javax.xml.namespace.QName;
import java.util.*;

import static de.richardliebscher.mdf4.blocks.metadata.Utils.defaultEmptyValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Text {
  @XmlMixed
  @XmlAnyElement(lax = true)
  private List<Object> content = new ArrayList<>();

  @XmlAttribute(name = "ci")
  private String ci = "0";

  @XmlAnyAttribute
  private Map<QName, String> otherAttributes = new HashMap<>();

  private Text() {
  }

  public Text(List<Object> content, String ci, Map<QName, String> otherAttributes) {
    this.content = defaultEmptyValue(content);
    this.ci = ci;
    this.otherAttributes = defaultEmptyValue(otherAttributes);
  }

  public List<Object> content() {
    return content;
  }

  public String ci() {
    return ci;
  }

  public Map<QName, String> otherAttributes() {
    return otherAttributes;
  }

  @Override
  public String toString() {
    return "TX{" +
            "content=" + content +
            ", ci='" + ci + '\'' +
            ", otherAttributes=" + otherAttributes +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Text extension = (Text) o;
    return Objects.equals(content, extension.content)
            && Objects.equals(ci, extension.ci)
            && Objects.equals(otherAttributes, extension.otherAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, ci, otherAttributes);
  }
}
