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

val faithful =
  project.in(file("faithful"))
    .enablePlugins(ScalaJSPlugin)

val `faithful-cats` =
  project.in(file("faithful-cats"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats" % "0.5.0",
        "org.typelevel"  %%% "cats-laws" % "0.5.0" % Test,
        "org.scalatest" %%% "scalatest" % "3.0.0-M7" % Test
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
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

val `benchmark-futures` =
  project.in(file("benchmarks/futures"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

val `benchmark-native` =
  project.in(file("benchmarks/native"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies ++= benchmarkDeps.value
    )
    .dependsOn(faithful)

val `faithful-project` =
  project.in(file("."))
    .aggregate(faithful, `faithful-cats`, `benchmark-faithful`, `benchmark-futures`, `benchmark-native`)

pomExtra in ThisBuild := (
  <url>http://github.com/julienrf/faithful</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:julienrf/faithful.git</url>
      <connection>scm:git:git@github.com:julienrf/faithful.git</connection>
    </scm>
    <developers>
      <developer>
        <id>julienrf</id>
        <name>Julien Richard-Foy</name>
        <url>http://julien.richard-foy.fr</url>
      </developer>
    </developers>
  )

val compileAllBenchmarks = taskKey[Unit]("Compile all benchmarks")

compileAllBenchmarks := {
  (fullOptJS in (`benchmark-faithful`, Compile)).value
  (fullOptJS in (`benchmark-futures`, Compile)).value
  (fullOptJS in (`benchmark-native`, Compile)).value
  ()
}

val publishDoc = taskKey[Unit]("Publish API documentation")

publishDoc := {
  IO.copyDirectory((doc in Compile).value, Path.userHome / "sites" / "julienrf.github.com" / "faithful" / version.value / "api")
}

import ReleaseTransformations._

releaseProcess in ThisBuild := Seq[ReleaseStep](checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges,
  ReleaseStep(action = Command.process("publishDoc", _))
)