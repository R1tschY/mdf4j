package de.richardliebscher.mdf4.utils;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class Cell<T> {
  private T value;

  public T get() {
    return value;
  }

  public void set(T value) {
    this.value = value;
  }
}
