package com.codepurls.mailytics.utils;

import java.util.List;
import java.util.regex.Pattern;

import edu.emory.mathcs.backport.java.util.Arrays;

public class StringUtils {
  static final Pattern CSV = Pattern.compile(",");
  
  public static String orEmpty(String str) {
    return str == null || str.isEmpty() ? "" : str;
  }
  public static boolean isBlank(String query) {
    return orEmpty(query).equals("");
  }
  
  @SuppressWarnings("unchecked")
  public static List<String> parseCSV(String moreLikeThisFields) {
    return Arrays.asList(CSV.split(moreLikeThisFields));
  }
}
