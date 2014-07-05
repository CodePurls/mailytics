package com.codepurls.mailytics.api.v1.transfer;

import java.util.List;

public class Page<T> {
  public int     totalHits, pageNum;
  public List<T> data;

  public static <T> Page<T> of(List<T> data, int totalHits, int pageNum) {
    Page<T> page = new Page<>();
    page.data = data;
    page.totalHits = totalHits;
    page.pageNum = pageNum;
    return page;
  }

  public static <T> Page<T> empty() {
    return Page.of(null, 0, 0);
  }
}
