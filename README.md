SciJava Maven Plugin
====================

scijava-maven-plugin is a Maven plugin for managing SciJava-based software.

Goals
-----

```shell
$ mvn scijava:help
[INFO] SciJava plugin for Maven 0.5.0
  A plugin for managing SciJava-based projects.

This plugin has 5 goals:

scijava:bump
  Bumps dependency and parent versions in SciJava projects.

scijava:eclipse-helper
  Runs the annotation processor of the scijava-common artifact even inside
  Eclipse.

scijava:help
  Display help information on scijava-maven-plugin.
  Call mvn scijava:help -Ddetail=true -Dgoal=<goal-name> to display parameter
  details.

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

It is recommended to enable the set-rootdir goal by making the
[SciJava POM](http://github.com/scijava/pom-scijava) the parent project:

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
    </executions>
  </plugin>
</plugins>
```
