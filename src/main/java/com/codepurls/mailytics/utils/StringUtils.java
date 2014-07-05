package com.codepurls.mailytics.utils;

import java.util.regex.Pattern;

public class StringUtils {
  static final Pattern CSV = Pattern.compile(",");
  public static String orEmpty(String str) {
    return str == null || str.isEmpty() ? "" : str;
  }
}
