package com.codepurls.mailytics.api.v1.providers;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codepurls.mailytics.config.Config.CORSConfig;

public class CORSFilter implements Filter {

  private CORSConfig cors;

  public CORSFilter(CORSConfig cors) {
    this.cors = cors;
  }

  public void init(FilterConfig filterConfig) throws ServletException {

  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;
    String allowedOrigin;
    if (cors.allowedOrigins.equals("any")) {
      allowedOrigin = req.getHeader("Origin");
    } else {
      allowedOrigin = cors.allowedOrigins;
    }
    resp.addHeader("Access-Control-Allow-Origin", allowedOrigin);
    resp.addHeader("Access-Control-Allow-Header", cors.allowedHeaders);
    resp.addHeader("Access-Control-Allow-Credentials", cors.allowCredentials.toString());

    if (req.getMethod().equalsIgnoreCase("OPTIONS")) {
      resp.addHeader("Access-Control-Allow-Methods", cors.allowedMethods);
    }
    chain.doFilter(request, response);
  }

  public void destroy() {

  }

}
