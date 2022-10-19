/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.dahldemo

import javax.inject.{Inject, Singleton}
import play.api.mvc.{ControllerComponents, Request}
import play.api.libs.json.{Json, JsValue, Reads}

import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import workflow._
import workflow.implicits._
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class Drinkmachine @Inject()(
  cc: ControllerComponents
)(implicit ec: ExecutionContext) extends DahlController(cc) {

  override val machineName = "drinkmachine"
  override val workflow =
    for {
      name     <- Workflow.step(
                    label = "name"
                  , get   = ()              => Form.input("Whats you name")
                  , post  = (body: JsValue) => Form.read[String](body)
                  )
      beverage <- Workflow.step(
                    label = "beverage"
                  , get   = ()              => Form.select("Select a drink", List("Tea", "Coffee"))
                  , post  = (body: JsValue) => Form.read[String](body)
                  )
      sugar    <- Workflow.step(
                    label = "sugar"
                  , get   = ()              => Form.select("How many sugars?", (0 to 3).toList)
                  , post  = (body: JsValue) => Form.read[String](body)
                  )
      hasMilk  <- Workflow.step(
                    label = "milk"
                  , get   = ()              => Form.select("Do you want milk", List("Yes", "No"))
                  , post  = (body: JsValue) => Form.read[String](body).map(_.map(_ == "Yes"))
                  )
      milkType <- if (hasMilk) {
                    Workflow.step(
                      label = "milk-type"
                    , get   = ()              => Form.select("What type of milk", List("Full", "Semi"))
                    , post  = (body: JsValue) => Form.read[String](body)
                    )
                  } else Workflow.pure("")
      _        <- Workflow.step(
                    label = "end"
                  , get   = ()              => Future.successful("")
                  , post  = (body: JsValue) => Future.successful(Right(()))
                  )
    } yield ()

  override val conf = WorkflowConf[Unit](
    workflow   = workflow
  , serialiser = DefaultSerialiser.serialiser
  , getBase    = routes.Drinkmachine.get("").url.dropRight(1)   // remove slash
  , postBase   = routes.Drinkmachine.post("").url.dropRight(1)  // remove slash
  )
}

abstract class DahlController(cc: ControllerComponents)(implicit ex: ExecutionContext) extends BackendController(cc) {

  protected val machineName: String
  protected val workflow: Workflow[Unit]
  protected val conf: WorkflowConf[Unit]

  private implicit val df = DahlFormat.dahlFormat

  def states()(implicit request: Request[_]) =
    request
      .session
      .get("dahl")
      .map(jsonStr => (Json.parse(jsonStr) \ machineName).as[Dahl])
      .fold(List.empty[Map[String, String]])(_.states)

  def get(stepId: String) = Action.async { implicit request =>
    WorkflowExecutor
      .getWorkflow[Unit, JsValue](conf, stepId, states())
      .map(dahl => Json.toJson(Map(machineName -> dahl)))
      .map(json => Ok(json).withSession(("dahl" -> json.toString)))
  }

  def post(stepId: String) = Action.async(parse.json) { implicit request =>
    WorkflowExecutor
      .postWorkflow(conf, stepId, request.body, states())
      .map(dahl => Json.toJson(Map(machineName -> dahl)))
      .map(json => Ok(json).withSession(("dahl" -> json.toString)))
  }
}

object DahlFormat {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  private implicit val controlFormat =
    ( (__ \ "method").format[String]
    ~ (__ \ "href"  ).format[String]
    ~ (__ \ "body"  ).formatNullable[String]
    )(Control.apply, unlift(Control.unapply))

  val dahlFormat =
    ( (__ \ "states"  ).format[List[Map[String, String]]]
    ~ (__ \ "controls").format[Map[String, Control]]
    )(Dahl.apply, unlift(Dahl.unapply))
}

object Form {
  private val fieldName = "value"

  def select[A](label: String, options: List[A]) = Future.successful(
    s"""["div",{"class":"form-group"},
        |  ["label",{"for":"$fieldName"},"$label"],
        |  ["select",{"class":"form-control","id":"$fieldName","name":"$fieldName","required":"required"},
        |    ["option",{"value":""},"$label"],
        |    ${options.map(x => s"""["option",{"value":"$x"},"$x"]""").mkString(",")}]]""".stripMargin
  )

  def input[A](label: String, inputType:String = "text") = Future.successful(
    s"""["div",{"class":"form-group"},
        |  ["label",{"for":"$fieldName"},"$label"],
        |  ["input",{"class":"form-control","id":"$fieldName","name":"$fieldName","required":"required","type":"$inputType"}]]""".stripMargin
  )

  def read[A](jsValue: JsValue)(implicit rds: Reads[A]) = Future.successful(
    (jsValue \ fieldName)
      .validate[A]
      .fold(_ => Left(s"Missing field '$fieldName'"), Right(_))
  )
}
