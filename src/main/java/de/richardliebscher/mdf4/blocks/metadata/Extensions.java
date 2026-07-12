package de.richardliebscher.mdf4.blocks.metadata;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.NonNull;

import java.util.*;
import java.util.function.Consumer;

import static de.richardliebscher.mdf4.blocks.metadata.Utils.defaultEmptyValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Extensions implements Iterable<Extension> {

  @XmlElement(name = "extension", required = true)
  private List<Extension> value;

  public Extensions() {
    value = new ArrayList<>();
  }

  public Extensions(List<Extension> value) {
    this.value = defaultEmptyValue(value);
  }

  @Override
  public @NonNull Iterator<Extension> iterator() {
    return value.iterator();
  }

  @Override
  public void forEach(Consumer<? super Extension> action) {
    value.forEach(action);
  }

  @Override
  public Spliterator<Extension> spliterator() {
    return value.spliterator();
  }

  @Override
  public String toString() {
    return "Extensions{" +
            "value=" + value +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Extensions that = (Extensions) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }
}
