import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.7.0"
  val hmrcMongoVersion = "0.73.0"

  val catsVersion  = "2.8.0"
  val circeVersion = "0.14.3"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion
  , "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoVersion

  // , "dahl" %% "dahl-workflow" % "0.1.0-SNAPSHOT"
  , "org.typelevel"  %% "cats-core"     % catsVersion
  , "org.typelevel"  %% "cats-free"     % catsVersion
  , "io.circe"       %% "circe-parser"  % circeVersion
  , "io.circe"       %% "circe-generic" % circeVersion
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-28"  % bootstrapVersion   % "test, it",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion   % Test,
    "org.mockito"       %% "mockito-scala-scalatest" % "1.17.5"           % Test,
  )
}
