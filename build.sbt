import sbt._
import Keys._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import sbtrelease._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys

// *****************************************************************************
// Projects
// *****************************************************************************

lazy val dilate =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .settings(settings)
    .settings(publishSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        library.scalaMeta, //% "compileonly"
        library.scalaCheck % Test,
        library.scalaTest  % Test
      )
    )

lazy val examples = project.in(file("examples"))
  .settings(settings)
  .settings(dontPublishSettings: _*)
  .dependsOn(dilate)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val scalaMeta  = "1.8.0"
      val scalaCheck = "1.13.4"
      val scalaTest  = "3.0.1"
    }
    val scalaMeta  = "org.scalameta"  %% "scalameta"  % Version.scalaMeta
    val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
    val scalaTest  = "org.scalatest"  %% "scalatest"  % Version.scalaTest
}

// *****************************************************************************
// Settings
// *****************************************************************************        |

lazy val settings =
  commonSettings ++
  gitSettings ++
  headerSettings

lazy val commonSettings =
  Seq(
    organization := "com.vitorsvieira",
    scalaVersion := "2.12.3",
    crossScalaVersions := Seq(scalaVersion.value, "2.11.11"),
    crossVersion := CrossVersion.binary,
    mappings.in(Compile, packageBin) +=
      baseDirectory.in(ThisBuild).value / "LICENSE" -> "LICENSE",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-encoding", "utf8",
      "-feature",
      "-explaintypes",
      "-target:jvm-1.8",
      "-language:implicitConversions",
      "-Ydelambdafy:method",
      "-Xcheckinit",
      "-Xfuture",
      "-Xlint",
      "-Xlint:-nullary-unit",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard"
    ),
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    ),
    unmanagedSourceDirectories.in(Compile) :=
      Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) :=
      Seq(scalaSource.in(Test).value),

    // New-style macro annotations are under active development.  As a result, in
    // this build we'll be referring to snapshot versions of both scala.meta and
    // macro paradise.
    //ivyConfigurations += config("compileonly").hide,
    //unmanagedClasspath in Compile ++= update.value.select(configurationFilter("compileonly")),
    resolvers += Resolver.url(
      "scalameta",
      url("http://dl.bintray.com/scalameta/maven"))(Resolver.ivyStylePatterns),
    // A dependency on macro paradise 3.x is required to both write and expand
    // new-style macros.  This is similar to how it works for old-style macro
    // annotations and a dependency on macro paradise 2.x.
    addCompilerPlugin(
      "org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full),
    scalacOptions += "-Xplugin-require:macroparadise",
    // temporary workaround for https://github.com/scalameta/paradise/issues/10
    scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
    // temporary workaround for https://github.com/scalameta/paradise/issues/55
    sources in (Compile, doc) := Nil, // macroparadise doesn't work with scaladoc yet.

    SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
        .setPreference(DoubleIndentConstructorArguments, true)
        .setPreference(RewriteArrowSymbols, true)
        .setPreference(AlignParameters, true)
        .setPreference(AlignArguments, true)
        .setPreference(DanglingCloseParenthesis, Preserve)
        .setPreference(SpacesAroundMultiImports, false),

      wartremoverWarnings ++= Warts.unsafe
)

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val headerSettings =
  headerLicense := Some(HeaderLicense.ALv2("2017", "Vitor S. Vieira"))

lazy val dontPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },

  //externalResolvers := resolvers.value,

  // Release settings
  publishMavenStyle             := true,
  publishArtifact in Test       := false,
  pomIncludeRepository          := { _ => false },

  releaseCrossBuild             := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,

  pomExtra := {
    <url>https://github.com/vitorsvieira/dilate</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:vitorsvieira/dilate.git</url>
        <connection>scm:git:git@github.com:vitorsvieira/dilate.git</connection>
      </scm>
      <developers>
        <developer>
          <id>vitorsvieira</id>
          <name>Vitor Vieira</name>
          <url>http://github.com/vitorsvieira</url>
        </developer>
      </developers>
  },

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

parallelExecution in Test := false
fork in Test := true

