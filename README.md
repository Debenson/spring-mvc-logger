spring-mvc-logger
=================
1. Add to pom.xml

```   
   <dependency>
        <groupId>com.gomore.experiment</groupId>
  			<artifactId>spring-mvc-logger</artifactId>
  			<version>0.0.1</version>
    </dependency>
```

2. Add to web.xml

```
    <filter>
        <filter-name>teeFilter</filter-name>
        <filter-class>com.gomore.experiment.logging.TeeFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>teeFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
```

3. Add to log4j.xml

```
   <logger name="com.gomore.experiment.logging.TeeFilter">
      <level value="DEBUG"/>
   </logger>
```
