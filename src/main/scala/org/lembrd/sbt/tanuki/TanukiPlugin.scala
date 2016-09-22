import com.typesafe.sbt.packager.archetypes.{JavaAppPackaging, TemplateWriter}
import com.typesafe.sbt.packager.linux.{LinuxFileMetaData, LinuxPackageMapping, LinuxPlugin}
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._
import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport._
import com.typesafe.sbt.packager.linux.LinuxPlugin.Users
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt._
import sbt.Keys._

object TanukiPlugin extends AutoPlugin {

  object autoImport extends TanukiPluginKeys

  import autoImport._

  import JavaAppPackaging.autoImport._

  override def requires = JavaAppPackaging && LinuxPlugin

  override lazy val projectSettings =
    inConfig(Debian)(envSettings)++
    inConfig(Debian)(Seq(
      daemonUser in Debian <<= daemonUser in Linux,
      daemonUserUid in Debian <<= daemonUserUid in Linux,
      daemonGroup in Debian <<= daemonGroup in Linux,
      daemonGroupGid in Debian <<= daemonGroupGid in Linux
    )) ++ tanukiSettings

  def envSettings = Seq(
    linuxMakeStartScript := None,
    linuxMakeStartScript in Linux := None,
    linuxMakeStartScript in Debian := None,
    daemonShell in Linux := "/bin/sh",
    daemonShell in Debian := "/bin/sh",
    debianPackageDependencies in Debian += "service-wrapper (>=3.5)",
    linuxPackageMappings <+= (makeInitScript, name in Linux ) map { (i, n) =>
      LinuxPackageMapping(Seq(i -> ("/etc/init.d/" + n)), LinuxFileMetaData(Users.Root, Users.Root, "755")).withConfig()
    }
  )

  def tanukiSettings = Seq(
    initScriptTemplateLocation := file("init_script.template"),
    appParameters := Nil,
    appClasspath := Seq("/usr/share/java/wrapper.jar"),

    appWorkingDir := defaultLinuxInstallLocation.value + "/" + (name in Linux).value,
    linuxScriptReplacements += ("appWorkingDir" -> appWorkingDir.value),
    linuxScriptReplacements += ("appLoggingDir" -> appLoggingDir.value),
    appClasspath <++= (appWorkingDir) map { (cwd) => Seq(cwd +"/*.jar", cwd +"/lib/*.jar")},
    appLoggingDir := "/var/log/" + (name in Linux).value,
    jvmHeapMin := 256,
    jvmHeapMax := 1000,
    tanukiConf := Nil,
    tanukiConf <++= (appWorkingDir, name in Linux, jvmHeapMin, jvmHeapMax, appLoggingDir, mainClass in Compile) map { (cwd, n, xms, xmx, logs, mainClass) => Seq(
      "wrapper.java.command" -> "java",
      "wrapper.app.parameter.1" -> mainClass.getOrElse(sys.error("no mainClass in Compile specified")),
      "wrapper.working.dir" -> cwd,
      "wrapper.statusfile" -> ("/var/run/" + n +".status"),
      "wrapper.environment.dump" -> "TRUE",
      "wrapper.java.mainclass" -> "org.tanukisoftware.wrapper.WrapperSimpleApp",
      "wrapper.java.initmemory" -> xms.toString,
      "wrapper.java.maxmemory" -> xmx.toString,
      "wrapper.logfile" -> (logs +"/wrapper.log"),
      "wrapper.logfile.format" -> "LPTM",
      "wrapper.logfile.loglevel" -> "INFO",
      "wrapper.logfile.maxsize" -> "100M",
      "wrapper.logfile.maxfiles" -> "2",
      "wrapper.syslog.loglevel" -> "NONE",
      "wrapper.console.title" -> ("Wrapper for " + n),
      "wrapper.debug" ->"FALSE",
      "wrapper.console.format" ->"LPTM",
      "wrapper.console.loglevel" -> "STATUS"
    )},
    bashScriptDefines := Nil,
    tanukiReplacements := Nil,
    extraTemplates := Nil,
    extraTemplatesProcessed := Nil,
    tanukiReplacements <+= (mainClass in Compile) map { "mainClass" -> _.getOrElse(sys.error("no mainClass in Compile specified"))  },
    tanukiReplacements <+= (daemonUser in Linux) map { "linuxUser" -> _ },
    tanukiReplacements <+= (daemonGroup in Linux) map { "linuxGroup" -> _ },
    tanukiReplacements <+= (name in Linux) map { "name" -> _ },
    tanukiReplacements <+= (appWorkingDir) map { "workingDir" -> _ },
    tanukiReplacements <+= (appLoggingDir) map { "loggingDir" -> _ },
    makeWrapperConf <<= (appParameters, tanukiConf, appClasspath, target in Universal) map {(appParams, conf, cp, t) =>
      val sb = new StringBuilder(
      s"""
         |# Generated by SBT tanukiPlugin
         |
         |# Application parameters:
         |
         |""".stripMargin)
      .append( getWrapperString( appParams.zipWithIndex.map{
        case(p, idx) => s"wrapper.java.additional.${idx+1}" -> p
      }) )
      .append(
      s"""
         |
         |# Classpath:
         |
         |""".stripMargin)
      .append( getWrapperString(cp.zipWithIndex.map{
        case(p, idx) => s"wrapper.java.classpath.${idx+1}" -> p
      }))
      .append(
      s"""
         |
         |# Wrapper parameters:
         |
         |""".stripMargin)
    .append( getWrapperString(conf) )

    val outPath = t / "tmp_extraTemplates" / "wrapper.conf"
    IO.write( outPath, sb.toString.getBytes() )
    outPath
  },
  extraTemplatesProcessed <++= (extraTemplates, tanukiReplacements, target in Universal, makeWrapperConf) map { (a,b,c, wrapper) =>
      a.map{
        case (mapping, file) => makeScript(file, b, c / "tmp_extraTemplates" / file.getName) -> mapping
      } ++ Seq(
        wrapper -> "conf/wrapper.conf"
      )
    },
    makeInitScript <<= (initScriptTemplateLocation, tanukiReplacements, stage, name in Linux) map { (a,b,c,d) => makeScript(a,b, c / "etc" / "init.d" / d ) },

    mappings in Universal <++= (packageBin in Compile, extraTemplatesProcessed ) map { (_, t) =>
      t
    },
    bashScriptEnvConfigLocation := None
  )

  private def getWrapperString( seq : Seq[(String,String)]) : String = {
    seq.map { case (k, v) => s"$k=$v" }.mkString("\n")
  }

  private def makeScript( template : File, replacements : Seq[(String, String)], target : File) : File = {
//    println("Processing template: " + template + " using:\n" + replacements.mkString("\n"))
    val bits = TemplateWriter.generateScript(resolveTemplate(template), replacements)

    IO.write(target, bits)
    target
  }

  private def resolveTemplate(defaultTemplateLocation: File): URL = {
    if (defaultTemplateLocation.exists)
      defaultTemplateLocation.toURI.toURL
    else
      getClass.getResource(defaultTemplateLocation.getName)
  }


}

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