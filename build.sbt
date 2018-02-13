import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import ReleaseTransformations._

import sbtcrossproject.{crossProject, CrossType}

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(buildSettings: _*)
  .settings(publishSettings: _*)
  .settings(scalaMacroDependencies: _*)
  .settings(moduleName := "kaleidoscope")

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val tests = project
  .in(file("tests"))
  .settings(buildSettings: _*)
  .settings(unmanagedSettings)
  .settings(moduleName := "kaleidoscope-tests")
  .dependsOn(coreJVM)

lazy val buildSettings = Seq(
  organization := "com.propensive",
  scalaVersion := "2.12.4",
  name := "kaleidoscope",
  version := "0.1.0",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Xfuture",
    "-Xexperimental",
    "-Ywarn-dead-code",
    "-Ywarn-nullary-unit",
    "-Ywarn-inaccessible",
    "-Ywarn-adapted-args"
  ),
  crossScalaVersions := Seq("2.11.11", "2.12.4"),
  scmInfo := Some(
    ScmInfo(url("https://github.com/propensive/kaleidoscope"),
            "scm:git:git@github.com:propensive/kaleidoscope.git")
  )
)

lazy val publishSettings = Seq(
  homepage := Some(url("http://gitub.com/propensive/kaleidoscope/")),
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
        <url>https://github.com/propensive/kaleidoscope/</url>
      </developer>
    </developers>
  ),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  ),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

import java.io.File

def crossVersionSharedSources() = Seq(
  (unmanagedSourceDirectories in Compile) ++= {
    (unmanagedSourceDirectories in Compile).value.map { dir: File =>
      new File(dir.getPath + "_" + scalaBinaryVersion.value)
    }
  }
)

lazy val unmanagedSettings = unmanagedBase := (scalaVersion.value
  .split("\\.")
  .map(_.toInt)
  .to[List] match {
  case List(2, 12, _) => baseDirectory.value / "lib" / "2.12"
  case List(2, 11, _) => baseDirectory.value / "lib" / "2.11"
})

lazy val quasiQuotesDependencies: Seq[Setting[_]] =
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq()
      case Some((2, 10)) =>
        Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
          "org.scalamacros" %% "quasiquotes" % "2.1.0" cross CrossVersion.binary
        )
    }
  }

lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies += "org.typelevel" %% "macro-compat" % "1.1.1",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  libraryDependencies += compilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
  )
)
