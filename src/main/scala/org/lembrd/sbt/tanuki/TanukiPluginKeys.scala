package org.lembrd.sbt.tanuki

import sbt.{SettingKey, TaskKey, _}

/**
  *
  * User: lembrd
  * Date: 22/09/16
  * Time: 13:57
  */
trait TanukiPluginKeys {
  val makeWrapperConf = TaskKey[File]("makeWrapperConf","Generate wrapper.conf")
  val tanukiConf = TaskKey[Seq[(String, String)]]("tanukiConf", "List of  wrapper.conf parameters")

  val jvmHeapMin = SettingKey[Int]("jvmHeapMin", "-Xms parameter in megabytes")
  val jvmHeapMax = SettingKey[Int]("jvmHeapMax", "-Xmx parameter in megabytes")

  val appParameters = TaskKey[Seq[String]]("appParameters", "List of application parameters")
  val appClasspath = TaskKey[Seq[String]]("appClasspath", "Classpath")

  val extraTemplatesProcessed = TaskKey[Seq[(File, String)]]("extraTemplatesProcessed", "process templates")
  val makeInitScript = TaskKey[File]("makeInitScript","Generate initScript")
  val tanukiReplacements = TaskKey[Seq[(String, String)]]("wrapperConfReplacements", "List of replacements in wrapper.conf template")
  val extraTemplates = TaskKey[Seq[(String, File)]]("extraTemplates", "List of templates with mappings to be processed. templatePath -> mapping")

  val initScriptTemplateLocation = TaskKey[File]("initScriptTemplateLocation", "The location of the init script template.")
  val appWorkingDir = SettingKey[String]("appWorkingDir", "Working Directory for the application")
  val appLoggingDir = SettingKey[String]("appLoggingDir", "Logging directory")
}
