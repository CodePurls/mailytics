package com.codepurls.mailytics.utils;

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

public class StringUtils {
  static final Pattern             CSV            = Pattern.compile(",");
  private static final String[]    STOP_WORD_LIST = { "re", "fwd", "fw" };
  private static final Set<String> STOP_WORDS     = new HashSet<>(Arrays.asList(STOP_WORD_LIST));

  public static String orEmpty(String str) {
    return str == null || str.isEmpty() ? "" : str;
  }

  public static String toSimpleText(String bodyPart) {
    return Jsoup.clean(bodyPart, Whitelist.simpleText()).replace("&nbsp;", " ");
  }

  public static String toPlainText(String htmlText, String contentType) {
    StringBuilder cleanText = new StringBuilder();
    boolean isHtml = !contentType.equals("text/plain");
    String text = isHtml ? Jsoup.clean(htmlText, new Whitelist().addTags("br")).replace("&nbsp;", " ") : htmlText;
    String[] chunks = isHtml ? text.split("\\<br") : text.split("\\r\\n");
    boolean breakLoop = false;
    for (String chunk : chunks) {
      chunk = chunk.trim();
      if(chunk.isEmpty())
        continue;
      if(!isHtml && chunk.startsWith(">"))
        breakLoop = true;
      else if(chunk.startsWith("/>")) {
        if(chunk.contains("From:") || chunk.contains("Original message") || chunk.contains("Original Appointment"))
          breakLoop = true;
        else if(chunk.contentEquals("/>") || chunk.contains("Sent from "))
          continue;
        else 
          cleanText.append(chunk.replace("/>", ""));
      }
      if(breakLoop)
        break;
      cleanText.append(chunk);
    }
    return cleanText.toString();
  }

  public static boolean isBlank(String str) {
    return orEmpty(str).equals("");
  }

  public static List<String> parseCSV(String str) {
    String[] strings = CSV.split(str);
    return Arrays.stream(strings).map(x -> x.trim()).filter(x -> !x.isEmpty()).collect(Collectors.toList());
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

  public static List<String> canonicalize(String string) {
    if (string == null || string.isEmpty()) return Collections.emptyList();

    List<String> strings = parseCSV(string);
    List<String> transformed = new ArrayList<>(strings.size());
    for (String str : strings) {
      String name, email;
      int emailStart = str.indexOf('<');
      if (emailStart == -1) {
        name = "";
        email = str;
      } else {
        name = str.substring(0, emailStart).trim();
        email = str.substring(emailStart + 1, str.length() - 1).trim();
      }
      transformed.add(format("%s %s", name, email));
    }
    return transformed;
  }

}
