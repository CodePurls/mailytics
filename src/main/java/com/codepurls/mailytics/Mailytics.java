package com.codepurls.mailytics;

import io.dropwizard.Application;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.skife.jdbi.v2.DBI;

import com.codepurls.mailytics.api.V1;
import com.codepurls.mailytics.api.v1.providers.CORSFilter;
import com.codepurls.mailytics.config.Config;
import com.codepurls.mailytics.service.dao.UserDao;
import com.codepurls.mailytics.service.index.IndexingService;
import com.codepurls.mailytics.service.security.UserService;

public class Mailytics extends Application<Config> {

  @Override
  public String getName() {
    return "Mail Analytics";
  }

  public void initialize(Bootstrap<Config> bootstrap) {

  }

  public void run(Config cfg, Environment env) throws Exception {
    DBIFactory factory = new DBIFactory();
    DBI dbi = factory.build(env, cfg.db, "db");
    UserDao userDao = dbi.onDemand(UserDao.class);
    UserService userService = new UserService(userDao);
    IndexingService indexingService = new IndexingService(cfg.index, userService);
    env.lifecycle().manage(indexingService);
    V1 v1 = new V1();
    env.jersey().register(v1);
    env.servlets().addFilter("CORSFilter", new CORSFilter(cfg.cors));
  }

  public static void main(String[] args) throws Exception {
    new Mailytics().run(args);
  }

}
