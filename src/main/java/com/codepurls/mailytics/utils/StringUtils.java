package com.codepurls.mailytics.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class StringUtils {
  static final Pattern             CSV            = Pattern.compile(",");
  private static final String[]    STOP_WORD_LIST = { "re", "fwd", "fw" };
  private static final Set<String> STOP_WORDS     = new HashSet<>(Arrays.asList(STOP_WORD_LIST));

  public static String orEmpty(String str) {
    return str == null || str.isEmpty() ? "" : str;
  }

  public static boolean isBlank(String str) {
    return orEmpty(str).equals("");
  }

  public static List<String> parseCSV(String str) {
    return Arrays.asList(CSV.split(str));
  }

  public static Iterable<String> tokenize(String line, boolean removeStopwords) {
    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
    try {
      TokenStream stream = analyzer.tokenStream("", line);
      stream.reset();
      CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
      return new Iterable<String>() {
        public Iterator<String> iterator() {
          return new Iterator<String>() {
            public boolean hasNext() {
              try {
                boolean incrementToken = stream.incrementToken();
                if (!incrementToken) analyzer.close();
                return incrementToken;
              } catch (IOException e) {
                return false;
              }
            }

            public String next() {
              String term = attr.toString();
              if (term.isEmpty() && hasNext()) return next();
              if (removeStopwords && isStopWord(term) && hasNext()) return next();
              else return term;
            }
          };
        }
      };
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isStopWord(String term) {
    return STOP_WORDS.contains(term);
  }
}
