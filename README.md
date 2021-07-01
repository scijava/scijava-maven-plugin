[![](https://github.com/scijava/scijava-maven-plugin/actions/workflows/build-main.yml/badge.svg)](https://github.com/scijava/scijava-maven-plugin/actions/workflows/build-main.yml)

[![](https://img.shields.io/maven-central/v/org.scijava/scijava-maven-plugin.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.scijava%22%20AND%20a%3A%22scijava-maven-plugin%22)

SciJava Maven Plugin
====================

scijava-maven-plugin is a Maven plugin for managing SciJava-based software.

Goals
-----

```shell
$ mvn scijava:help
[INFO] SciJava plugin for Maven 1.1.0
  A plugin for managing SciJava-based projects.

This plugin has 7 goals:

scijava:bump
  Bumps dependency and parent versions in SciJava projects.

scijava:populate-app
  Copies .jar artifacts and their dependencies into a SciJava application
  directory structure. ImageJ 1.x plugins (identified by containing a
  plugins.config file) get copied to the plugins/ subdirectory and all other
  .jar files to jars/. However, you can override this decision by setting the
  scijava.app.subdirectory property to a specific subdirectory. It expects the
  location of the SciJava application directory to be specified in the
  scijava.app.directory property (which can be set on the Maven command-line).
  If said property is not set, the populate-app goal is skipped.

scijava:eclipse-helper
  Runs the annotation processor of the scijava-common artifact even inside
  Eclipse.

scijava:help
  Display help information on scijava-maven-plugin.
  Call mvn scijava:help -Ddetail=true -Dgoal=<goal-name> to display parameter
  details.

scijava:install-artifact
  Downloads .jar artifacts and their dependencies into a SciJava application
  directory structure. ImageJ 1.x plugins (identified by containing a
  plugins.config file) get copied to the plugins/ subdirectory and all other
  .jar files to jars/. However, you can override this decision by setting the
  scijava.app.subdirectory property to a specific subdirectory. It expects the
  location of the SciJava application directory to be specified in the
  scijava.app.directory property (which can be set on the Maven command-line).
  If said property is not set, the install-artifact goal is skipped.

scijava:set-rootdir
  Sets the project.rootdir property to the top-level directory of the current
  Maven project structure.

scijava:verify-no-snapshots
  Mojo wrapper for the SnapshotFinder.
  Parameters:

  - failFast - end execution after first failure (default: false)
  - groupIds - an inclusive list of groupIds. Errors will only be reported for
    projects whose groupIds are contained this list. (default: empty - all
    groupIds considered)
  - groupId - Singular groupIds option. Will be appended to groupIds if both are
    specified.
```

Usage
-----

It is recommended to enable the _set-rootdir_ as well as the _populate-app_
goal by making the [SciJava POM](http://github.com/scijava/pom-scijava)
the parent project:

```xml
<project ...>
  <parent>
    <groupId>org.scijava</groupId>
    <artifactId>pom-scijava</artifactId>
    <version>7.5.2</version>
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
      <execution>
        <id>populate-app</id>
        <phase>install</phase>
        <goals>
          <goal>populate-app</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
</plugins>
```
