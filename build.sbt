name := "ElasticDegradator"

version := "0.1"

scalaVersion := "2.12.8"

lazy val doobieVersion = "0.7.0"
lazy val http4sVersion = "0.20.10"
lazy val pureConfigVersion = "0.10.2"

scalacOptions := Seq(
  "-feature",
  "-deprecation",
  "-explaintypes",
  "-unchecked",
  "-Xfuture",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:existentials",
  "-Ypartial-unification",
  //"-Xfatal-warnings",
  "-Xlint:-infer-any,_",
  "-Ywarn-value-discard",
  "-Ywarn-numeric-widen",
  "-Ywarn-extra-implicit",
  //"-Ywarn-unused:_",
  "-Ywarn-inaccessible",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-opt:l:inline"
)

libraryDependencies ++= Seq(

  //zio
  "dev.zio" %% "zio" % "1.0.0-RC12-1",
  "dev.zio" %% "zio-streams" % "1.0.0-RC12-1",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC3",
  "org.typelevel" %% "cats-effect" % "2.0.0",

  //http4s
  "org.asynchttpclient" % "async-http-client" % "2.10.3",
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-async-http-client" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "io.circe" %% "circe-generic" % "0.12.0-RC4",

  //doobie
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-h2" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,

  //db
  "com.h2database" % "h2" % "1.4.99",
  "org.flywaydb" % "flyway-core" % "5.2.4",
  "org.slf4j" % "slf4j-log4j12" % "1.7.26",

  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,

  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4")
)