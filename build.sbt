// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}
import com.typesafe.sbt.pgp.PgpKeys.publishSigned

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .settings(buildSettings: _*)
  .settings(publishSettings: _*)
  .settings(scalaMacroDependencies: _*)
  .settings(moduleName := "kaleidoscope")
  .settings(
    scalaVersion := crossScalaVersions.value.head
  )
  .jvmSettings(
    crossScalaVersions := "2.12.4" :: "2.13.0-M3" :: "2.11.12" :: Nil
  )
  .jsSettings(
    crossScalaVersions := "2.12.4" :: "2.11.12" :: Nil
  )
  .nativeSettings(
    crossScalaVersions := "2.11.12" :: Nil
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val root = (project in file("."))
  .aggregate(coreJVM, coreJS, coreNative)
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val buildSettings = Seq(
  organization := "com.propensive",
  name := "kaleidoscope",
  version := "0.2.0",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Xfuture",
    "-Xexperimental",
    "-Ywarn-value-discard",
    "-Ywarn-dead-code",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-inaccessible",
    "-Ywarn-adapted-args"
  ),
  scmInfo := Some(
    ScmInfo(url("https://github.com/propensive/kaleidoscope"),
            "scm:git:git@github.com:propensive/kaleidoscope.git")
  )
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/propensive/kaleidoscope/")),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  autoAPIMappings := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <developers>
      <developer>
        <id>propensive</id>
        <name>Jon Pretty</name>
        <url>https://github.com/propensive/</url>
      </developer>
    </developers>
  )
)

lazy val unmanagedSettings = unmanagedBase := (scalaVersion.value
  .split("\\.")
  .map(_.toInt)
  .to[List] match {
  case List(2, 12, _) => baseDirectory.value / "lib" / "2.12"
  case List(2, 11, _) => baseDirectory.value / "lib" / "2.11"
})

lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
)
