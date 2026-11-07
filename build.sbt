scalaVersion := "3.3.3"

name := "fitness-energy-balance"
version := "0.1.0"

fork := true
scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test
