package org.daron

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import cats.syntax.functor._

/**
  * Application based on Monix Task
  */
object ApplicationMonix extends TaskApp with Application {

  override def run(args: List[String]): Task[ExitCode] = program[Task].use(_ => Task.never.as(ExitCode.Success))

}
