/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package workflow

import scala.concurrent.{ExecutionContext, Future}

object WorkflowExecutor {

  import implicits._

  private def mkWorkflowContext[A,B](
    conf         : WorkflowConf[A]
  , label        : String
  , previousLabel: Option[String]
  , optB         : Option[B]
  ): WorkflowContext[B] = WorkflowContext(
    actionCurrent  = label // conf.router.post(label)
  , actionPrevious = previousLabel //previousLabel.map(conf.router.post(_))
  , stepObject     = optB
  , restart        = conf.restart // conf.router.get(conf.restart)
  , goto           = s => s //conf.router.get(s)
  )

  def getWorkflow[A, BODY](conf: WorkflowConf[A], stepId: String, states: List[Map[String, String]])
      (implicit ec: ExecutionContext): Future[Dahl] =
    doGet[A, BODY](conf, stepId, None, conf.workflow, states).flatMap {
      case Some(r) => Future(r)
      case None    => sys.error(s"TODO doGet($conf, $stepId, None, ${conf.workflow}) returned None") //  postWorkflow(conf, stepId)
    }

  // is tail recursive since recursion happens asynchronously
  private def doGet[A, BODY](conf: WorkflowConf[A], targetLabel: String, previousLabel: Option[String], remainingWf: Workflow[A], states: List[Map[String, String]])
      (implicit ec: ExecutionContext): Future[Option[Dahl]] =
    fold[A, B forSome {type B}, Option[Dahl], BODY](conf, targetLabel, previousLabel, remainingWf, states)(
      { case (step, optB, ctx) =>
          step.get().map(inputs => Some(Dahl(states = states, controls = Map(step.label -> Control(method = "post", href = s"${conf.postBase}/${step.label}", inputs = Some(inputs))))))
      },
      { case (step, Some(b), ctx) =>
          doGet(conf, targetLabel, if (step.cache) previousLabel else Some(step.label), step.next(b), states)
        case (step, None   , ctx) =>
          step.get().map(inputs => Some(Dahl(states = states, controls = Map(step.label -> Control(method = "post", href = s"${conf.postBase}/${step.label}", inputs = Some(inputs))))))
      }
    )

  def postWorkflow[A, BODY](conf: WorkflowConf[A], stepId: String, body: BODY, states: List[Map[String, String]])
      (implicit ec: ExecutionContext): Future[Dahl] =
    doPost(conf, stepId, None, conf.workflow, body, states)

  // is tail recursive since recursion happens asynchronously
  private def doPost[A, BODY](conf: WorkflowConf[A], targetLabel: String, previousLabel: Option[String], remainingWf: Workflow[A], body: BODY, states: List[Map[String, String]])
      (implicit ec: ExecutionContext): Future[Dahl] = {
    fold[A, B forSome {type B}, Dahl, BODY](conf, targetLabel, previousLabel, remainingWf, states)(
      { case (step, optB, ctx) =>
          step.post(body).flatMap {
            case Left(r)  =>
              step.get().map(inputs => Dahl(states = states, controls = Map(step.label -> Control(method = "post", href = s"${conf.postBase}/${step.label}", inputs = Some(inputs)))))
            case Right(a) =>
              val idx       = states.flatMap(_.keys).indexOf(step.label)
              val newStates = (if (idx == -1) states else states.take(idx)) :+ Map(step.label -> step.serialiser.serialise(a))
              nextStep(step.next(a)).flatMap {
                case Some(next) => next.get().map(inputs => Dahl(states = newStates, controls = Map(next.label -> Control(method = "post", href = s"${conf.postBase}/${next.label}", inputs = Some(inputs)))))
                case None       => sys.error("doPost: flow finished!")
              }
          }
      },
      { case (step, Some(b), ctx) =>
          doPost(conf, targetLabel, if (step.cache) previousLabel else Some(step.label), step.next(b), body, states)
        case (step, None   , ctx) =>
          step.get().map(inputs => Dahl(states = states, controls = Map(step.label -> Control(method = "post", href = s"${conf.postBase}/${step.label}", inputs = Some(inputs)))))
      }
    )
    }

  private def fold[A, B, C, BODY]
    (conf: WorkflowConf[A], targetLabel: String, previousLabel: Option[String], remainingWf: Workflow[A], states: List[Map[String, String]])
    (f: (WorkflowSyntax.WSStep[B, BODY, Workflow[A]], Option[B], WorkflowContext[B]) => Future[C]
    ,g: (WorkflowSyntax.WSStep[B, BODY, Workflow[A]], Option[B], WorkflowContext[B]) => Future[C]
    )(implicit ec: ExecutionContext): Future[C] =
    remainingWf.resume.flatMap {
      case Right(a) => sys.error("doGet: flow finished!") // flow has finished (only will happen if last step has a post)
      case Left(step: WorkflowSyntax.WSStep[B @unchecked, BODY @unchecked, Workflow[A] @unchecked]) => // TODO avoid unchecked?
        val optB: Option[B] =
            states
              .flatMap(_.get(step.label))
              .headOption
              .flatMap(b => step.serialiser.deserialise(b))

        lazy val ctx = mkWorkflowContext(conf, step.label, previousLabel, optB)
        if (step.label == targetLabel) f(step, optB, ctx)
        else                           g(step, optB, ctx)
    }

  private def nextStep[A](wf: Workflow[A])(implicit ec: ExecutionContext) =
    wf.resume.map {
      case Left(step: WorkflowSyntax.WSStep[_, _, _]) => Some(step)
      case err                                        => None
    }
}
