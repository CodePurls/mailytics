package com.codepurls.mailytics.utils;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

public class Tuple<K, V> extends SimpleImmutableEntry<K, V> implements Entry<K, V> {
  private static final long serialVersionUID = -7826192909891256563L;

  public Tuple(K key, V value) {
    super(key, value);
  }

  public static <K, V> Tuple<K, V> of(K k, V v) {
    return new Tuple<K, V>(k, v);
  }

}
