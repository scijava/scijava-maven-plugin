SciJava Maven Plugin
====================

scijava-maven-plugin is a Maven plugin for managing SciJava-based software.

It provides one goal:

* __set-rootdir__ (as part of the _validate_ phase of the life cycle): finds the project root
  directory of nested Maven projects and sets the __rootdir__ property to point there. This goal is
  useful if you want to define the location of the _ImageJ.app/_ directory relative to the project
  root directory.

It is recommended to enable it automatically by making the
[SciJava POM](http://github.com/scijava/pom-scijava) the parent project:

```xml
<project ...>
  <parent>
    <groupId>org.scijava</groupId>
    <artifactId>pom-scijava</artifactId>
    <version>1.156</version>
  </parent>
  ...
</project>
```

Alternatively, you can include the plugin explicitly in the life cycle:

```xml
<plugins>
  <!-- Set the rootdir property to the root of the multi-module project -->
  <plugin>
    <groupId>org.scijava</groupId>
    <artifactId>scijava-maven-plugin</artifactId>
    <version>0.1.0</version>
    <executions>
      <execution>
        <id>set-rootdir</id>
        <phase>validate</phase>
        <goals>
          <goal>set-rootdir</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
</plugins>
```
