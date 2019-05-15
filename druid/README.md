Welcome to the Druid wiki!

[Requirements](#markdown-header-0-requirements)

[Setup & Build](#markdown-header-1-setup-build)

[Run Our Test Suite](#markdown-header-2-run-our-test-suite)

[Run Java Analysis (console)](#markdown-header-3-run-java-analysis-console)

[Run Java Analysis (eclipse)](#markdown-header-4-run-java-analysis-eclipse)

[Run Android Analysis](#markdown-header-5-run-android-analysis)

[Run Java & Android Analysis in GUI Launcher](#markdown-header-6-run-java-android-analysis-in-gui-launcher)

#0 Requirements
To get full functionality, you need the followings installed:

* jdk1.8+

* python2.7+ (python 3 compatible)

* ant

#1 Setup & Build
##1.1 Setup
### setup JAVA_HOME forJDK1.8

```
export JAVA_HOME=/PathToJDK/jdk1.8.0_121
export CLASSPATH=.:$JAVA_HOME/lib:$JAVA_HOME/jre/lib
export PATH=$JAVA_HOME/bin:$PATH
```

###setup for Linux
After clone this repository, you can setup this repo by exec the following command in the repo root directory:
```
. setup
```
###setup for Windows(NT):
double click "setup.exe"

##1.2 Build
###(Re)Compile (if you have changed the src)
by typing
```
ant
```
###Compile tests (if you want to run our testcases)
```
ant tests
```
###Generate runnable jar (if you want to package this (exclude android front end))
```
ant jar
```

#2 Run Our Test Suite
###run all tests with an option
```
sh runtests -result
```
###run a single test with an option
```
sh runtests pta.objectsensitivity.ObjectSensitivity -kobjsens 1
```

##In eclipse
For eclipse users, run the Test class(e.g. pta.PTAValidator) in JUnit mode.

#3 Run Java Analysis (console)
> Note that here the examples are executed in Linux. For windows, the separator '/' should be changed to '\'
##3.1 Template
```
python druid.py -apppath <APPCLASSPATH or APPJAR> -mainclass <Mainclass> [options]
```
##3.2 Example
###Benchmark
```
python druid.py -apppath benchmarks/dacapo-bench/antlr.jar -mainclass dacapo.antlr.Main
```
###Application
```
python druid.py -apppath benchmarks/dacapo/jar/bootstrap.jar -libpath benchmarks/dacapo/jar/bootstrap -mainclass org.apache.catalina.startup.Bootstrap
```
###TestCase (reflection without string analysis)
```
python druid.py -reflection -apppath testclasses -mainclass stringanalysis.testee.BasicMethodCall 
```
###TestCase (with string analysis)
```
python druid.py -reflection -stringanalysis -apppath testclasses -mainclass stringanalysis.testee.BasicMethodCall 
```


#4 Run Java Analysis (eclipse)
add the following run args in "run configurations.."
##4.1 Template
```
-jre <JRE> -apppath <APPCLASSPATH or APPJAR> -mainclass <Mainclass> [options]
```
##4.2 Example
###Benchmark
```
-jre jre/jre1.6.0_45 -apppath ../benchmarks/dacapo-bench/antlr.jar -mainclass dacapo.antlr.Main
```
###Application
```
-jre jre/jre1.6.0_45 -apppath ../benchmarks/dacapo/jar/bootstrap.jar -libpath ../benchmarks/dacapo/jar/bootstrap -mainclass org.apache.catalina.startup.Bootstrap
```
###TestCase(reflection without string analysis)
```
-jre jre/jre1.6.0_45 -apppath testclasses -mainclass stringanalysis.testee.BasicMethodCall -reflection
```
###TestCase(with string analysis)
```
-jre jre/jre1.6.0_45 -apppath testclasses -mainclass stringanalysis.testee.BasicMethodCall -reflection -stringanalysis
```


#5 Run Android Analysis
##5.1 Template
```
python druid.py -apk <APKFILE> [options]
```
##5.2 Example
###Droidbench
```
python druid.py -apk benchmarks/droidbench/1_ActivityCommunication2.apk
```

#6 Run Java & Android Analysis in GUI Launcher
###Launch on Linux
Launch "Launcher"
###Launch on Windows
Start "Launcher_x64.exe" or "Launcher_x86.exe"

Have fun!