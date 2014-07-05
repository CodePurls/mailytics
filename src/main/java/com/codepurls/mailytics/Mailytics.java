package com.codepurls.mailytics;

import io.dropwizard.Application;
import io.dropwizard.auth.oauth.OAuthProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.skife.jdbi.v2.DBI;

import com.codepurls.mailytics.api.V1;
import com.codepurls.mailytics.api.v1.auth.MailyticsAuthenticator;
import com.codepurls.mailytics.api.v1.providers.CORSFilter;
import com.codepurls.mailytics.config.Config;
import com.codepurls.mailytics.service.EventLogService;
import com.codepurls.mailytics.service.dao.EventLogDao;
import com.codepurls.mailytics.service.dao.UserDao;
import com.codepurls.mailytics.service.index.IndexingService;
import com.codepurls.mailytics.service.search.SearchService;
import com.codepurls.mailytics.service.security.UserService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class Mailytics extends Application<Config> {
  private static Mailytics instance;

  public String getName() {
    return "Mail Analytics";
  }

  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(new MigrationsBundle<Config>() {
      public DataSourceFactory getDataSourceFactory(Config configuration) {
        return configuration.db;
      }
    });
  }

  public void run(Config cfg, Environment env) throws Exception {
    DBIFactory factory = new DBIFactory();
    DBI dbi = factory.build(env, cfg.db, "db");
    EventLogService eventLogService = new EventLogService(dbi.onDemand(EventLogDao.class));
    UserService userService = new UserService(eventLogService, dbi.onDemand(UserDao.class));
    IndexingService indexingService = new IndexingService(cfg.index, userService);
    SearchService searchService = new SearchService(indexingService, userService);
    env.lifecycle().manage(indexingService);
    configureJersey(env);
    configureAPI(cfg, env, indexingService, searchService);

    instance = this;
  }

  private void configureAPI(Config cfg, Environment env, IndexingService indexingService, SearchService searchService) {
    env.jersey().register(new V1(indexingService, searchService));
    env.jersey().register(new OAuthProvider<>(new MailyticsAuthenticator(indexingService.getUserService()), "mailytics.com"));
    env.servlets().addFilter("CORSFilter", new CORSFilter(cfg.cors));
  }

  private void configureJersey(Environment env) {
    env.getObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    env.getObjectMapper().setSerializationInclusion(Include.NON_NULL);
  }

  public static Mailytics getInstance() {
    return instance;
  }

  public static void main(String[] args) throws Exception {
    new Mailytics().run(args);
  }

}
