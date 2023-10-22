#import "@preview/tablex:0.0.5": tablex, rowspanx, colspanx
#import "template.typ": *

#show: project.with(
  title: "Java Project Analyser",
  authors: (
    (name: "Zhang Yao", email: "zhang@cinea.cc"),
  ),
)

#outline(
    title: "Table of Contents",
    indent: 2em
)

#pagebreak()

= Overview

== Team Members

#table(
    columns: (auto, auto, auto, auto),
    inset: 10pt,
    align: horizon,
    [*Name*],[*Matr. No.*],[*Contact number*],[*Email address*],
    [Zhang Yao],[2152955],[+86 13518772062],[zhang\@cinea.cc],
    [Tang Shuhan],[],[],[],
    [Wang Shuyu],[2151894],[+86 17709584390],[1491456253@qq.com],
    [Tang Kexian],[],[],[]
)
_No specific order in the table._

== Description

== Features

= Usage

== Requirement

A Java environment that is *greater than or equal to 17 is required*. We use *Amazon Corretto 17 LTS* in our development and test. But other JDK distribution is also compatiable. Learn more about Amazon Corretto: #link("https://aws.amazon.com/corretto",[https://aws.amazon.com/corretto])

== Compile our project

We use *Maven* to manage our project and its dependencies. A *Maven Wrapper* instance is also included in our project repository. You can just using it to compile our project in most cases. Learn more about Maven Wrapper: #link("https://maven.apache.org/wrapper/",[https://maven.apache.org/wrapper/])

We suggest checking your JDK version before compiling our project:

```shell-unix-generic
${JAVA_HOME}/bin/java -version    # Never continue if not 17 or higher.
```

To start the compilation process, please execute the following command:

```shell-unix-generic
./mvnw -B dependency:go-offline
./mvnw -B package
```

The first command will try to download all dependencies our project used from the Maven Central Repository. You may use Aliyun Maven Central Repository in China to accelerate the downloading process. Learn more about Aliyun Maven Central Repository: #link("https://developer.aliyun.com/mvn/guide",[https://developer.aliyun.com/mvn/guide]).

After finishing the compilation, you may find the artifact in `target` directory, which looks like `java-project-analyser-cli-0.0.1-SNAPSHOT.jar`. You may move the output to a convenient location and change its name as needed. Assuming we have renamed it to `analyser.jar` in convience in the following parts.

== Simple usage

Just use `java -jar` command to launch our project and use it:

```shell-unix-generic
java -jar ./analyser.jar
```

Our project provides a REPL user interface, in which you can easily input commands, obtain results, and reuse the previous state instead of having to restart the program after each command execution. Our project utilizes the *Spring Shell* framework to provide you with an exceptional interactive command-line experience. Learn more about Spring Shell: #link("https://spring.io/projects/spring-shell",[https://spring.io/projects/spring-shell]).

Enter `help` to get a list of functionalities provided by our project.

```shell-unix-generic
shell:>help
```

== Open a project

#important([
    Our project does not support libraries that generate and process code at compile-time, such as *Lombok*.

    If you want to analyse a project which uses Lombok, consider *Delombok* it manually before analysing. Learn more about the Delombok feature: #link("https://projectlombok.org/features/delombok",[https://projectlombok.org/features/delombok])
])

Use `open` command to open, load and analyse a Java project:

```shell-unix-generic
shell:>open /usr/src/sample-project/src/main/java
```

*Tips:*

- The path should contain the root package, but not the root package self.
- On Windows, please double the backslashes in the path or change them to forward slashes. For example, both `C:/commons-lang/src/main/java` and `C:\\commons-lang\\src\\main\\java` are correct.
- The process of opening the project should be completed within one second, and it should not exceed ten seconds at most. In our test, open a huge project with 200 classes and 3000 methods usually cost about 2 seconds.

== Querying method invocation relationships

== Querying method parameter relationships

== Automaticly fixing syntax errors

= Design

== System architecture and components

== General workflow

= Implementation

== Major data structures

== Efficiency considerations

== Key techniques

= Test

== Test Cases

- Case A: The most basic case



= Conclusion
