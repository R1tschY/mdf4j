package de.richardliebscher.mdf4.blocks.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Utils {
  static <T> List<T> defaultEmptyValue(List<T> value) {
    return value != null ? value : List.of();
  }

  static <K, V> Map<K, V> defaultEmptyValue(Map<K, V> value) {
    return value != null ? value : Map.of();
  }
}
