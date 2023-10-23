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
    [Tang Shuhan],[2153877],[+86 15300862015],[2405353202\@qq.com],
    [Wang Shuyu],[2151894],[+86 17709584390],[1491456253\@qq.com],
    [Tang Kexian],[2152240],[+86 18707016886],[1135530278\@qq.com]
)
_No specific order in the table._

== Description

This project offers analysis functionality for Java projects. Users can conveniently obtain the call relationship of specified methods in a project using the software provided by this project. This includes a list of methods that a given method calls, as well as a list of methods that call the given method. The software can also trace call chains to a specified depth. Additionally, it provides tracking for the sources of function arguments within a method, assisting users in locating the origins of these arguments. If the arguments are provided in the form of literals, the software will also directly provide their values.

== Features

Here is a feature list which is implemented by our team: 

- REPL-style command-line interaction
- Import and organize Java projects according to package structure
- Import large-scale Java projects within an acceptable timeframe
- Check for the presence of syntax errors when importing projects
- Parallelize reading, importing, and analyzing projects
- Provide user-friendly output for time-consuming operations
- List all files, classes and interfaces, and methods in the project
- List all instances of a specific class within the project
- Find all other methods called by a specified method
- Identify all instances where a specific method is called within the project
- Trace all methods that could potentially call a specified method
- Locate and trace all possible argument sources for a specified method and provide values for literal argument sources.

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

Example output:

```
shell:>open D:\\java-project-analyser-sample\\src\\main\\java
Please wait.
Open project success: 
                       1 packages found.
                       1 files found.
                       1 classes and interfaces found.
                       3 methods found.
                       3ms used.
Successfully indexed methods and classes in 0ms.
```

== Querying method invocation relationships

Use `func` command to query the method invocation relationships of a specified method:

```shell-unix-generic
shell:>func <class reference> <method name> <optional: searching deepth>
```

For example:

```shell-unix-generic
shell:>func main.Test sayHello
```

Example output:

```
shell:>func main.Test sayHello
Please wait.
****************** Invokes ******************
	->(java.io.PrintStream) println
*********************************************

****************** Invoked by ******************
	<-(main.Test) introduction
			<-(main.Test) main
	<-(main.Test) main
************************************************

Time cost:
		Get Invokes: 4 ms
		Get Invoked: 6 ms
```

== Querying method parameter sources

Use `param` command to query the parameter sources of a specified method:

```shell-unix-generic
shell:>param <class reference> <method name> <optional: searching deepth>
```

For example:

```shell-unix-generic
shell:>func main.Test sayHello
```

Example output:

```
shell:>param main.Test sayHello
Please wait.
***********Parameter Origin***********
	String name:
			  "Garfield": (main.Test) introduction
			  "Jon": (main.Test) sayHello
			  name1: (main.Test) introduction
				 <- "Odie": (main.Test) main
**************************************

Time cost: 2 ms
```

== Automaticly fixing syntax errors

_to be implemented_

= Design

== System architecture and components

Our system is built on top of the Spring Boot framework. We utilize the *Spring Framework* to organize and manage the lifecycle of Beans and dependency injection. We employ Spring Shell to create an easy-to-use interactive command-line interface.

Specifically, our system is mainly divided into the following parts: 

- *Service interfaces and their implementations*, which are responsible for providing the core functions of the project, such as importing the project, parsing the method call relationships, and tracing back the sources of parameters.
  
- *Model classes*, which are the data structures used by the core business of the project. They abstract and model business information and store this information.
  
- *Helper classes*, which are tools used during the execution of the project's core business. They operate without any state, provide only static services, and are not instantiated into class instances.
  
- DTOs (*Data Transfer Objects*), which are the data structures used for interaction between the core business classes and helper classes. These data structures don't store business-related models but are responsible solely for pure information transfer.

#figure(
  image(
    "img/sys-arch.drawio.png",
    width: 80%
  ),
  caption: [System architecture]
) 

== General workflow

= Implementation

== Major data structures

== Efficiency considerations

== Key techniques

= Test

== Test Cases

- Case A: The most basic case

- Case B: Large-scale Java library case

We have chosen the commons-lang library, open-sourced by the Apache Foundation, as our test project. Apache Commons is an excellent project within the Java ecosystem, offering high-quality and reliable components for almost every aspect of Java. This library consists of over two hundred classes and more than three thousand methods, totaling 175,000 lines of code. Choosing such a vast project to test our own is a significant challenge. The version we selected is located at commit gbe417ff07.



= Conclusion
