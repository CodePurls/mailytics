package com.codepurls.mailytics.data.search;

public class WordAndCount {
  public String word;
  public int    count;

  public WordAndCount(String key, Integer value) {
    word = key;
    count = value;
  }
}
