/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package workflow

case class Control(method: String, href: String, inputs: Option[String])
case class Dahl(states: List[Map[String, String]], controls: Map[String, Control])
