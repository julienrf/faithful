organization in ThisBuild := "org.julienrf"

scalaVersion in ThisBuild := "2.12.4"
val crossScalaV = Seq("2.11.11", "2.12.4")

scalacOptions in ThisBuild ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Xexperimental"
)

val publishSettings = Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  pomExtra :=
    <developers>
      <developer>
        <id>julienrf</id>
        <name>Julien Richard-Foy</name>
        <url>http://julien.richard-foy.fr</url>
      </developer>
    </developers>,
  scalacOptions in (Compile, doc) ++= Seq(
    "-doc-source-url", s"https://github.com/julienrf/${name.value}/tree/v${version.value}€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  ),
  apiURL := Some(url(s"http://julienrf.github.io/${name.value}/${version.value}/api/")),
  autoAPIMappings := true,
  homepage := Some(url(s"https://github.com/julienrf/${name.value}")),
  licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/julienrf/${name.value}"),
      s"scm:git:git@github.com:julienrf/${name.value}.git"
    )
  )
)

val noPublishSettings = Seq(
  publishArtifact := false,
  publish := (()),
  publishLocal := (())
)

val faithful =
  project.in(file("faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := crossScalaV)

val `faithful-cats` =
  project.in(file("faithful-cats"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings: _*)
    .settings(
      crossScalaVersions := crossScalaV,
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats-core" % "1.1.0",
        "org.typelevel"  %%% "cats-laws" % "1.1.0" % Test,
        "org.typelevel" %%% "discipline" % "0.8" % Test,
        "org.scalatest" %%% "scalatest" % "3.0.4" % Test
      )
    )
    .dependsOn(faithful)

val benchmarkDeps = Def.setting {
  Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.5"
  )
}

val `benchmark-faithful` =
  project.in(file("benchmarks/faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(noPublishSettings: _*)
    .settings(
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

val `benchmark-futures` =
  project.in(file("benchmarks/futures"))
    .enablePlugins(ScalaJSPlugin)
    .settings(noPublishSettings: _*)
    .settings(
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

val `benchmark-native` =
  project.in(file("benchmarks/native"))
    .enablePlugins(ScalaJSPlugin)
    .settings(noPublishSettings: _*)
    .settings(
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

import ReleaseTransformations._

val `faithful-project` =
  project.in(file("."))
    .settings(noPublishSettings: _*)
    .settings(
//      releaseCrossBuild := true,
//      releaseProcess := Seq[ReleaseStep](checkSnapshotDependencies,
//        inquireVersions,
//        runClean,
//        releaseStepCommand("+faithful/test"),
//        releaseStepCommand("+faithful-cats/test"),
//        setReleaseVersion,
//        commitReleaseVersion,
//        tagRelease,
//        releaseStepCommand("+faithful/publishSigned"),
//        releaseStepCommand("+faithful-cats/publishSigned"),
//        setNextVersion,
//        commitNextVersion,
//        pushChanges
//      )
    )
    .aggregate(faithful, `faithful-cats`, `benchmark-faithful`, `benchmark-futures`, `benchmark-native`)

val compileAllBenchmarks = taskKey[Unit]("Compile all benchmarks")

compileAllBenchmarks := {
  (fullOptJS in (`benchmark-faithful`, Compile)).value
  (fullOptJS in (`benchmark-futures`, Compile)).value
  (fullOptJS in (`benchmark-native`, Compile)).value
  ()
}
