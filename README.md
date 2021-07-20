[![](https://github.com/scijava/scijava-packages-plugin/actions/workflows/build-main.yml/badge.svg)](https://github.com/scijava/scijava-packages-plugin/actions/workflows/build-main.yml)

[![](https://img.shields.io/maven-central/v/org.scijava/scijava-packages-plugin.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.scijava%22%20AND%20a%3A%22scijava-packages-plugin%22)

SciJava Packages Plugin
====================

scijava-packages-plugin provides a set of [Maven Enforcer Plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/) rules for policing the package hierarchy at build time.

## Usage

Currently, the only way to utilize these rules is by explicitly declaring it in the life cycle

```xml
<plugin>
	<artifactId>maven-enforcer-plugin</artifactId>
	<dependencies>
		<dependency>
			<groupId>org.scijava</groupId>
			<artifactId>scijava-packages-plugin</artifactId>
			<version>0-SNAPSHOT</version>
		</dependency>
	</dependencies>
	<executions>
		...
	</executions>
</plugin>
```

Rules
====================

# No Package Cycles
[Circular dependencies](https://en.wikipedia.org/wiki/Circular_dependency) are usually considered poor practice. To prevent circular dependencies, add the following `execution`:

```xml
<execution>
	<id>enforce-no-package-cycles</id>
	<goals>
		<goal>enforce</goal>
	</goals>
	<phase>test</phase>
	<configuration>
		<rules>
			<NoPackageCyclesRule
				implementation="org.scijava.packages.plugin.NoPackageCyclesRule" />
		</rules>
	</configuration>
</execution>
```

### Including test classes

If you want to exclude tests from cycle checking, you can use the parameter `includeTests` which is set to true by default:

```xml
        ...
        <rules>
            <NoPackageCyclesRule implementation="org.scijava.packages.plugin.NoPackageCyclesRule">
                <includeTests>false</includeTests>
            </NoPackageCyclesRule>
        </rules>
        ...
```

### Restricting scope

**:warning: Only use this, if there is no other way! Once there are exceptions, the connection between those excluded packages
will grow stronger and stronger, without notice!**

If you want to exclude packages or restrict check to certain packages only, you can use `includedPackages` or `excludedPackages` (although you really should not!):

```xml
        ...
        <rules>
            <NoPackageCyclesRule implementation="org.scijava.packages.plugin.NoPackageCyclesRule">
                <includedPackages>
                    <includedPackage>myapp.code.good</includedPackage>
                </includedPackages>
            </NoPackageCyclesRule>
        </rules>
        ...
```

```xml
        ...
        <rules>
            <NoPackageCyclesRule implementation="de.andrena.tools.nopackagecycles.NoPackageCyclesRule">
                <excludedPackages>
                    <excludedPackage>myapp.code.bad</excludedPackage>
                </excludedPackages>
            </NoPackageCyclesRule>
        </rules>
        ...
```


# No Subpackage Dependence
Subpackage Dependence can throw a wrench into libraries wishing to follow the [Dependency Inversion principle](https://en.wikipedia.org/wiki/Dependency_inversion_principle). To prevent subpackage dependence, add the following `execution`:

```xml
<execution>
	<id>enforce-no-subpackage-dependence</id>
	<goals>
		<goal>enforce</goal>
	</goals>
	<phase>test</phase>
	<configuration>
		<rules>
			<NoSubpackageDependenceRule
				implementation="org.scijava.packages.plugin.NoSubpackageDependenceRule" />
		</rules>
	</configuration>
</execution>
```


## See also

* The original version by Daniel Seidewitz on [Stackoverflow](http://stackoverflow.com/questions/3416547/maven-jdepend-fail-build-with-cycles). Improved by showing all packages afflicted with cycles and the corresponding classes importing the conflicting packages; this version was written [here](https://github.com/andrena/no-package-cycles-enforcer-rule). From there, the SciJava team made the behavior more extensible, making it easier to write and use more package-based rules.
* [JDepend](https://github.com/clarkware/jdepend), the library being used to detect package cycles.
* For more information about package cycles, see ["The Acyclic Dependencies Principle" by Robert C. Martin (Page 6)](http://www.objectmentor.com/resources/articles/granularity.pdf). 
* The [Maven Enforcer Plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/)
