// See LICENSE for license details.

import chiselBuild.ChiselDependencies.{basicDependencies, chiselLibraryDependencies, chiselProjectDependencies}
import chiselBuild.ChiselSettings

site.settings

site.includeScaladoc()

ghpages.settings

import UnidocKeys._

val internalName = "chisel3"

name := internalName

// The Chisel projects we're dependendent on.
val dependentProjects: Seq[String] = basicDependencies(internalName)

lazy val customUnidocSettings = unidocSettings ++ Seq (
  doc in Compile := (doc in ScalaUnidoc).value,
  target in unidoc in ScalaUnidoc := crossTarget.value / "api"
)

lazy val commonSettings = ChiselSettings.commonSettings ++ Seq (
  version := "3.1-SNAPSHOT",
  git.remoteRepo := "git@github.com:ucb-bar/chisel3.git",
  autoAPIMappings := true,
  scalacOptions := Seq("-deprecation", "-feature"),
  // Use the root project's unmanaged base for all sub-projects.
  unmanagedBase := (unmanagedBase in root).value,
  // Use the root project's classpath for all sub-projects.
  fullClasspath := (fullClasspath in Compile in root).value,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  // Use the root project's unmanaged base for all sub-projects.
  unmanagedBase := (unmanagedBase in root).value,
  // Since we want to examine the classpath to determine if a dependency on firrtl is required,
  //  this has to be a Task setting.
  //  Fortunately, allDependencies is a Task Setting, so we can modify that.
  allDependencies := allDependencies.value ++ chiselLibraryDependencies(dependentProjects)

)

lazy val publishSettings = ChiselSettings.publishSettings ++ Seq (
  pomExtra := <url>http://chisel.eecs.berkeley.edu/</url>
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/ucb-bar/chisel3.git</url>
      <connection>scm:git:github.com/ucb-bar/chisel3.git</connection>
    </scm>
    <developers>
      <developer>
        <id>jackbackrack</id>
        <name>Jonathan Bachrach</name>
        <url>http://www.eecs.berkeley.edu/~jrb/</url>
      </developer>
    </developers>,

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
    "com.github.scopt" %% "scopt" % "3.5.0"
  ),

  // Tests from other projects may still run concurrently.
  parallelExecution in Test := true,

  javacOptions ++= Seq("-target", "1.7")
)

lazy val coreMacros = (project in file("coreMacros")).
  settings(commonSettings: _*).
  settings(publishArtifact := false)

lazy val chiselFrontend = (project in file("chiselFrontend")).
  settings(commonSettings: _*).
  settings(publishArtifact := false).
  dependsOn(coreMacros).
  dependsOn((chiselProjectDependencies(dependentProjects)):_*)

// There will always be a root project.
lazy val root = RootProject(file("."))

lazy val chisel3 = (project in file(".")).
  settings(commonSettings: _*).
  settings(customUnidocSettings: _*).
  settings(publishSettings: _*).
  // Prevent separate JARs from being generated for coreMacros and chiselFrontend.
  dependsOn(coreMacros % "compile-internal;test-internal").
  dependsOn(chiselFrontend % "compile-internal;test-internal").
  // The following is required until sbt-scoverage correctly deals with inDependencies
  aggregate(coreMacros, chiselFrontend).
  settings(
    scalacOptions in Test ++= Seq("-language:reflectiveCalls"),
    scalacOptions in Compile in doc ++= Seq(
      "-diagrams",
      "-diagrams-max-classes", "25",
      "-doc-version", version.value,
      "-doc-title", name.value,
      "-doc-root-content", baseDirectory.value+"/root-doc.txt"
    ),
    aggregate in doc := false,
    // Include macro classes, resources, and sources main JAR.
    mappings in (Compile, packageBin) ++= (mappings in (coreMacros, Compile, packageBin)).value,
    mappings in (Compile, packageSrc) ++= (mappings in (coreMacros, Compile, packageSrc)).value,
    mappings in (Compile, packageBin) ++= (mappings in (chiselFrontend, Compile, packageBin)).value,
    mappings in (Compile, packageSrc) ++= (mappings in (chiselFrontend, Compile, packageSrc)).value,
    // Export the packaged JAR so projects that depend directly on Chisel project (rather than the
    // published artifact) also see the stuff in coreMacros and chiselFrontend.
    exportJars := true
  ).
  dependsOn((chiselProjectDependencies(dependentProjects)):_*)
