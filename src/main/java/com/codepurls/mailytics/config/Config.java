package com.codepurls.mailytics.config;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import java.io.File;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config extends Configuration {

  @Valid
  @NotNull
  @JsonProperty
  public CORSConfig        cors  = new CORSConfig();

  @Valid
  @NotNull
  @JsonProperty
  public IndexConfig       index = new IndexConfig();

  @Valid
  @NotNull
  @JsonProperty
  public DataSourceFactory db    = new DataSourceFactory();

  public static class CORSConfig {
    @JsonProperty
    public String  allowedOrigins   = "any";
    @JsonProperty
    public String  allowedHeaders   = "Accept, Authorization, Content-Type, Last-Modified, Origin, X-Requested-With, Cookie";
    @JsonProperty
    public Boolean allowCredentials = Boolean.TRUE;
    @JsonProperty
    public String  allowedMethods   = "GET, POST, PUT, DELETE, HEAD, OPTIONS";
  }

  public static class IndexConfig {
    @JsonProperty
    public String location       = System.getProperty("user.home", System.getProperty("java.io.tmpdir")) + File.separatorChar + "mailyics";
    @JsonProperty
    public int    indexerThreads = 2;
    @JsonProperty
    public int    indexQueueSize = 1024;
  }
}
