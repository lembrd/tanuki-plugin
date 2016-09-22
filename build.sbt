name := "tanuki-plugin"

version := "0.4"

organization := "org.lembrd"

scalaVersion := "2.10.5"

sbtPlugin := true

bintrayPackageLabels := Seq("tanuki-plugin")

publishArtifact in Test := false

licenses += ("GPL-2.0", url("https://opensource.org/licenses/GPL-3.0"))

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")