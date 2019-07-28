name := "hotel-price-drops"

version := "0.1"

scalaVersion := "2.12.8"

val doobieVersion = "0.7.0"
val circeVersion = "0.11.1"
val http4sVersion = "0.20.6"

libraryDependencies ++= Seq(
  "org.seleniumhq.selenium" % "selenium-java" % "3.141.59",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.typelevel" %% "cats-effect" % "1.3.1",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-core" % http4sVersion,
  "org.http4s" %% "http4s-client" % http4sVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-java8" % circeVersion,
  "javax.mail" % "mail" % "1.5.0-b01",
  "com.typesafe" % "config" % "1.3.4",
  "com.iheart" %% "ficus" % "1.4.7",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.tpolecat"               %% "doobie-core"             % doobieVersion,
  "org.tpolecat"               %% "doobie-hikari"           % doobieVersion,
  "org.tpolecat"               %% "doobie-postgres"         % doobieVersion,
  "org.flywaydb"               % "flyway-core"              % "4.2.0"

)