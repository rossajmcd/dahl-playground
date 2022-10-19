/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package workflow

import scala.concurrent.{ExecutionContext, Future}
import cats.{Applicative, Monad}
import cats.free.FreeT

case class WorkflowConf[A](
  workflow  : Workflow[A]
, serialiser: Serialiser[A]
, start     : String = "start"
, restart   : String = "restart"
, getBase   : String
, postBase  : String
)

case class WorkflowContext[A] private [workflow] (
  actionCurrent : String
, actionPrevious: Option[String]
, stepObject    : Option[A]
, restart       : String
, goto          : String => String
)

private [workflow] sealed trait WorkflowSyntax[+Next]
private [workflow] object WorkflowSyntax {
  case class WSStep[A, BODY, Next](label: String, get: () => Future[String], post: BODY => Future[Either[String, A]], serialiser: Serialiser[A], cache: Boolean, next: A => Next) extends WorkflowSyntax[Next]
}

trait WorkflowInstances {
  implicit def catsMonadForWorkflow(implicit M0: Monad[Future], A0: Applicative[Future]) =
    new WorkflowMonad { implicit val M = M0; implicit val A = A0 }

  private [workflow] sealed trait WorkflowMonad extends Monad[Workflow] {
    implicit def M: Monad[Future]
    implicit def A: Applicative[Future]

    override def flatMap[A, B](fa: Workflow[A])(f: A => Workflow[B]): Workflow[B] =
      fa.flatMap(f)

    override def tailRecM[A, B](a: A)(f: A => Workflow[Either[A,B]]): Workflow[B] =
      FreeT.tailRecM(a)(f)

    override def pure[A](x: A): Workflow[A] =
      FreeT.pure[WorkflowSyntax, Future, A](x)
  }
}

/** exposes Monadic functions directly on Workflow (fixed for FreeT[Future])
 *  rather than depending on cats (Applicative[Workflow].pure)
 */
object Workflow {

  import cats.implicits._

  /** Wraps a Step as a Workflow so can be composed in a Workflow
   *  @param label the name of the step. Is used in URLs to identify the current step,
   *         and as the key to the result when stored - needs to be unique in the workflow
   *  @param step the step to be included in the workflow
   *  @param reader defines how to read the step result out of the session
   *  @param writer defines how to write the step result to the session
   */
  def step[A, B](label: String, get: () => Future[String], post: B => Future[Either[String, A]])(implicit serialiser: Serialiser[A], ec: ExecutionContext): Workflow[A] =
    FreeT.liftF[WorkflowSyntax, Future, A](WorkflowSyntax.WSStep[A,B,A](label, get, post, serialiser, false, identity))

  def cache[A, B](label: String, get: () => Future[String], post: B => Future[Either[String, A]])(implicit serialiser: Serialiser[A], ec: ExecutionContext): Workflow[A] =
    FreeT.liftF[WorkflowSyntax, Future, A](WorkflowSyntax.WSStep[A,B,A](label, get, post, serialiser, true, identity))

  def liftF[A](f: => Future[A])(implicit ec: ExecutionContext): Workflow[A] =
    FreeT.liftT(f)

  def pure[A](x: A)(implicit ec: ExecutionContext): Workflow[A] =
    FreeT.pure[WorkflowSyntax, Future, A](x)
}

trait AllInstances
  extends WorkflowInstances
  with    DefaultSerialiser
  with    cats.instances.FutureInstances
object implicits extends AllInstances
