
organization := "zamblauskas"

name := "jstatplot"

version := "0.7.1"

scalaVersion := "2.11.12"

resolvers += Resolver.bintrayRepo("zamblauskas", "maven")

libraryDependencies ++= Seq(
  "zamblauskas"        %% "scalaplot"        % "0.4.1",
  "zamblauskas"        %% "scala-csv-parser" % "0.11.0",
  "com.twitter"        %% "util-core"        % "6.30.0",
  "com.github.scopt"   %% "scopt"            % "3.3.0",
  "org.scalaz"         %% "scalaz-effect"    % "7.2.0",
  "org.apache.commons" %  "commons-lang3"    % "3.5"
)

enablePlugins(JavaAppPackaging)
