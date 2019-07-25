name := "hotel-price-drops"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.seleniumhq.selenium" % "selenium-java" % "3.141.59",
  "dev.profunktor" %% "redis4cats-effects" % "0.8.3",
  "dev.profunktor" %% "redis4cats-log4cats" % "0.8.3",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.typelevel" %% "cats-effect" % "1.3.1",
  "org.http4s" %% "http4s-dsl" % "0.20.6",
  "org.http4s" %% "http4s-core" % "0.20.6",
  "org.http4s" %% "http4s-client" % "0.20.6",
  "io.circe" %% "circe-core" % "0.11.1",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-parser" % "0.11.1",
  "javax.mail" % "mail" % "1.5.0-b01",
  "com.typesafe" % "config" % "1.3.4",
  "com.iheart" %% "ficus" % "1.4.7"
)