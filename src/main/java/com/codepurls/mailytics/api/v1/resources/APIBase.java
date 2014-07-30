package com.codepurls.mailytics.api.v1.resources;

import io.dropwizard.auth.Auth;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.codepurls.mailytics.data.search.Request;
import com.codepurls.mailytics.data.search.Request.Resolution;
import com.codepurls.mailytics.data.search.Request.SortDirecton;
import com.codepurls.mailytics.data.search.Request.SortType;
import com.codepurls.mailytics.data.security.User;
import com.codepurls.mailytics.service.security.UserService;
import com.codepurls.mailytics.utils.StringUtils;

public class APIBase {
  protected static final String PARAM_QUERY        = "q";
  protected static final String PARAM_PAGE         = "page";
  protected static final String PARAM_SORT         = "sort";
  protected static final String PARAM_SORT_DIR     = "sort_dir";
  protected static final String PARAM_MLT_FIELDS   = "mlt_fields";
  protected static final String PARAM_SIZE         = "size";
  protected static final String PARAM_START_DATE   = "start_date";
  protected static final String PARAM_END_DATE     = "end_date";
  protected static final String PARAM_DEFAULT_PAGE = "1";
  protected static final String PARAM_DEFAULT_SIZE = "10";
  protected static final String PARAM_RESOLUTION   = "res";

  @Context
  protected UserService         userService;

  @Auth(required = false)
  protected User                user;

  @QueryParam(PARAM_QUERY)
  protected String              query;

  @DefaultValue(PARAM_DEFAULT_SIZE)
  @QueryParam(PARAM_SIZE)
  protected int                 size;

  @QueryParam(PARAM_START_DATE)
  protected String              startDateStr;
  @QueryParam(PARAM_END_DATE)
  protected String              endDateStr;

  @DefaultValue(PARAM_DEFAULT_PAGE)
  @QueryParam(PARAM_PAGE)
  protected int                 page;

  @QueryParam(PARAM_SORT)
  protected String              sort;

  @QueryParam(PARAM_SORT_DIR)
  protected String              sortDir;

  @QueryParam(PARAM_MLT_FIELDS)
  protected String              moreLikeThisFields;

  @QueryParam(PARAM_RESOLUTION)
  protected String              facetResolution;

  protected Request createRequest(List<Integer> mbIds) {
    Request r = new Request();
    r.mailboxIds = mbIds;
    r.query = query;
    r.pageNum = page;
    r.pageSize = size;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    if(StringUtils.isBlank(startDateStr)) {
      r.startTime = new Date(0L).getTime();
    }else {
      r.startTime = LocalDate.parse(startDateStr, formatter).atTime(LocalTime.of(0, 0)).toInstant(ZoneOffset.MIN).toEpochMilli();
    }
    if(StringUtils.isBlank(endDateStr)) {
      r.endTime = new Date().getTime();
    }else {
      r.endTime = LocalDate.parse(endDateStr, formatter).atTime(LocalTime.of(0, 0)).toInstant(ZoneOffset.MIN).toEpochMilli();
    }
    r.sort = StringUtils.isBlank(sort) ? SortType.DATE : SortType.valueOf(sort.toUpperCase());
    r.sortDir = StringUtils.isBlank(sortDir) ? SortDirecton.ASC : SortDirecton.valueOf(sortDir.toUpperCase());
    if (r.mailboxIds == null || r.mailboxIds.isEmpty()) {
      r.mailboxIds = userService.getMailboxes(user).stream().map((m) -> m.id).collect(Collectors.toList());
    }
    if (!StringUtils.isBlank(moreLikeThisFields)) {
      r.similarFields = StringUtils.parseCSV(moreLikeThisFields);
    }
    if (!StringUtils.isBlank(facetResolution)) {
      r.resolution = Resolution.valueOf(facetResolution.toUpperCase());
    }
    return r;
  }

}
