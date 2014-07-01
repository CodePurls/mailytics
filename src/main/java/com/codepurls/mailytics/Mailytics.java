package com.codepurls.mailytics;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.codepurls.mailytics.api.V1;
import com.codepurls.mailytics.api.v1.providers.CORSFilter;
import com.codepurls.mailytics.config.Config;

public class Mailytics extends Application<Config> {

  @Override
  public String getName() {
    return "Mail Analytics";
  }

  public void initialize(Bootstrap<Config> bootstrap) {

  }

  public void run(Config cfg, Environment env) throws Exception {
    V1 v1 = new V1();
    env.jersey().register(v1);
    env.servlets().addFilter("CORSFilter", new CORSFilter(cfg.cors));
  }

  public static void main(String[] args) throws Exception {
    new Mailytics().run(args);
  }

}
