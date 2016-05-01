organization in ThisBuild := "org.julienrf"

scalaVersion in ThisBuild := "2.11.8"

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

val faithful =
  project.in(file("faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings: _*)

val `faithful-cats` =
  project.in(file("faithful-cats"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats" % "0.5.0",
        "org.typelevel"  %%% "cats-laws" % "0.5.0" % Test,
        "org.typelevel" %%% "discipline" % "0.5" % Test,
        "org.scalatest" %%% "scalatest" % "3.0.0-M15" % Test
      )
    )
    .dependsOn(faithful)

val benchmarkDeps = Def.setting {
  Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.0"
  )
}

val `benchmark-faithful` =
  project.in(file("benchmarks/faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      publishArtifact := false,
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

val `benchmark-futures` =
  project.in(file("benchmarks/futures"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      publishArtifact := false,
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

val `benchmark-native` =
  project.in(file("benchmarks/native"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      publishArtifact := false,
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

import ReleaseTransformations._

val `faithful-project` =
  project.in(file("."))
    .settings(
      publishArtifact := false,
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      releaseProcess := Seq[ReleaseStep](checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        publishArtifacts,
        ReleaseStep(action = Command.process("publishDoc", _)),
        setNextVersion,
        commitNextVersion,
        ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
        pushChanges
      )
    )
    .aggregate(faithful, `faithful-cats`, `benchmark-faithful`, `benchmark-futures`, `benchmark-native`)

val compileAllBenchmarks = taskKey[Unit]("Compile all benchmarks")

compileAllBenchmarks := {
  (fullOptJS in (`benchmark-faithful`, Compile)).value
  (fullOptJS in (`benchmark-futures`, Compile)).value
  (fullOptJS in (`benchmark-native`, Compile)).value
  ()
}

val publishDoc = taskKey[Unit]("Publish API documentation")

publishDoc := {
  IO.copyDirectory((doc in (faithful, Compile)).value, Path.userHome / "sites" / "julienrf.github.com" / "faithful" / version.value / "api")
  IO.copyDirectory((doc in (`faithful-cats`, Compile)).value, Path.userHome / "sites" / "julienrf.github.com" / "faithful-cats" / version.value / "api")
}
