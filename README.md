# tanuki-plugin
[![Build Status](https://travis-ci.org/lembrd/tanuki-plugin.svg)](https://travis-ci.org/lembrd/tanuki-plugin)
[![Download](https://api.bintray.com/packages/lembrd/sbt-plugins/tanuki-plugin/images/download.svg) ](https://bintray.com/lembrd/sbt-plugins/tanuki-plugin/_latestVersion)

This extension plugin for sbt-native-packager (https://github.com/sbt/sbt-native-packager) 
generate wrapper.conf for Tanuki Service Wrapper (http://wrapper.tanukisoftware.com/doc/english/introduction.html)

# Usage

* plugins.sbt
```scala
resolvers += Resolver.bintrayIvyRepo("lembrd", "sbt-plugins")
addSbtPlugin("org.lembrd" % "tanuki-plugin" % "0.4")
```
* build.sbt
```scala
enablePlugins(JDebPackaging, JavaAppPackaging, TanukiPlugin)
settings(
    mainClass in Compile := Some("xxx.Server"),
    jvmHeapMin := 2000,
    jvmHeapMax := 2000,
    extraTemplates <+= (sourceDirectory) map { (src) =>
      "conf/logback.xml" -> src / "debian" / "logback.xml.template"
    },
    appParameters <++= (appWorkingDir, appLoggingDir, name in Debian) map { (cwd, logs, name) => Seq(
      "-Djava.net.preferIPv4Stack=true",
      "-Dconfig=/etc/" + name + "/application.conf",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+UseG1GC",
      "-XX:MaxGCPauseMillis=100",
      "-XX:+ParallelRefProcEnabled",
      "-XX:-ResizePLAB",
      "-XX:ParallelGCThreads=14",
      "-XX:G1NewSizePercent=4",
      "-Dlogback.configurationFile="+cwd+"/conf/logback.xml",
      "-XX:+PrintGCTimeStamps",
      "-Xloggc:"+logs+"/gc-%WRAPPER_TIME_YYYYMMDDHHIISS%.log",
      "-XX:+PrintGCDateStamps",
      "-XX:+UseGCLogFileRotation",
      "-XX:GCLogFileSize=100m",
      "-XX:NumberOfGCLogFiles=10",
      "-XX:+PrintGCApplicationStoppedTime",
      "-XX:+PrintAdaptiveSizePolicy"
    )}
)
```