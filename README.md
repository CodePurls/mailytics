mailytics - EMail Analytics
=========

Over the years, people have accumulated hundreds of thousands of emails from different organizations and mail services.
Mailyics aims to liberate those emails from different types of proprietary services, tools and stores to one central place for search and analytics.



User Guide
----------
See Project Wiki - https://github.com/CodePurls/mailytics/wiki

Building & Running
--------
    mvn clean package
    java -jar maliytics.jar

Dependencies
------------
- Apache Lucene (High-performance search library) - http://lucene.apache.org
- Java-libpst (Microsoft PST file parser) - https://github.com/rjohnsondev/java-libpst
- Java Mail - https://javamail.java.net
- MSTOR Java Mail Provider - http://sourceforge.net/projects/mstor/
- Dropwizard Application framework - https://dropwizard.github.io/dropwizard
- Jersey (JAX-RS) API - https://jersey.java.net/
- H2 database - http://www.h2database.com
