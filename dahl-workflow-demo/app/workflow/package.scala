/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

import scala.concurrent.Future
import cats.Functor
import cats.free.FreeT

package object workflow {

  private [workflow] implicit val workflowSyntaxFunctor: Functor[WorkflowSyntax] = new Functor[WorkflowSyntax] {
    def map[A, B](fa: WorkflowSyntax[A])(f: A => B): WorkflowSyntax[B] = fa match {
      case step: WorkflowSyntax.WSStep[_, _, A] => step.copy(next = step.next.andThen(f))
    }
  }

  /** Defines a sequence of steps to be executed */
  type Workflow[A] = FreeT[WorkflowSyntax, Future, A]

}
