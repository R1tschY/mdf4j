package de.richardliebscher.mdf4.blocks.metadata;

import de.richardliebscher.mdf4.blocks.metadata.properties.PropertyContainer;
import jakarta.xml.bind.annotation.*;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "FHcomment")
public class FileHistoryComment extends ElementBase {
  @XmlElement(name = "TX")
  private Text comment;

  @XmlElement(name = "tool_id", required = true)
  private BaseName toolId;

  @XmlElement(name = "tool_vendor", required = true)
  private BaseName toolVendor;

  @XmlElement(name = "tool_version", required = true)
  private BaseName toolVersion;

  @XmlElement(name = "tool_version")
  private BaseName userName;

  @XmlElement(name = "common_properties")
  private PropertyContainer commonProperties;

  @XmlElement(name = "extensions")
  private Extensions extensions;

  @XmlAnyAttribute
  private Map<QName, String> rootAttributes = new HashMap<>();

  public Text comment() {
    return comment;
  }

  public BaseName toolId() {
    return toolId;
  }

  public BaseName toolVendor() {
    return toolVendor;
  }

  public BaseName toolVersion() {
    return toolVersion;
  }

  public BaseName userName() {
    return userName;
  }

  public PropertyContainer commonProperties() {
    return commonProperties;
  }

  public Extensions extensions() {
    return extensions;
  }

  public Map<QName, String> rootAttributes() {
    return rootAttributes;
  }

  @Override
  public String toString() {
    return "FHcomment{" +
            "comment=" + comment +
            ", toolId=" + toolId +
            ", toolVendor=" + toolVendor +
            ", toolVersion=" + toolVersion +
            ", userName=" + userName +
            ", commonProperties=" + commonProperties +
            ", extensions=" + extensions +
            ", rootAttributes=" + rootAttributes +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    FileHistoryComment that = (FileHistoryComment) o;
    return Objects.equals(comment, that.comment)
            && Objects.equals(toolId, that.toolId)
            && Objects.equals(toolVendor, that.toolVendor)
            && Objects.equals(toolVersion, that.toolVersion)
            && Objects.equals(userName, that.userName)
            && Objects.equals(commonProperties, that.commonProperties)
            && Objects.equals(extensions, that.extensions)
            && Objects.equals(rootAttributes, that.rootAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
            comment, toolId, toolVendor, toolVersion, userName, commonProperties, extensions, rootAttributes);
  }
}
