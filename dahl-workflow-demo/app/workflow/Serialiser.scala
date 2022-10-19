/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package workflow

/** Defines how to serialise a single step object to/from String */
trait Serialiser[A] {
  def serialise(a: A): String
  def deserialise(s: String): Option[A]
}

/** serialiser which uses circe for storing step results in session */
trait DefaultSerialiser {
  implicit def serialiser[A](implicit e: io.circe.Encoder[A], d: io.circe.Decoder[A]): Serialiser[A] =
    new Serialiser[A] {
      import io.circe.syntax._

      def serialise(a: A): String =
        a.asJson.toString

      def deserialise(s: String): Option[A] = {
        io.circe.parser.decode[A](s) match {
          case Left(e)  => play.api.Logger.apply(getClass).warn(s"Error deserializing session cookie: $e"); None
          case Right(o) => Some(o)
        }
      }
    }
}


object DefaultSerialiser extends DefaultSerialiser
