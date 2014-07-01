package com.codepurls.mailytics;

import com.codepurls.mailytics.config.Config;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class Mailytics extends Application<Config> {

  public void initialize(Bootstrap<Config> bootstrap) {

  }

  public void run(Config cfg, Environment env) throws Exception {
  }
  
  public static void main(String[] args) throws Exception {
    new Mailytics().run(args);
  }
  
}
