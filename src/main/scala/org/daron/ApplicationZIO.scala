package org.daron

import zio.clock.Clock
import zio.{TaskR, UIO, ZIO}
import zio.interop.catz._

object ApplicationZIO extends CatsApp with Application {

  type AppTask[A] = TaskR[Clock, A]

  override def run(args: List[String]) = program[AppTask]
    .use(_ => ZIO.never.map(_ => 0))
    .foldM(
      _ => UIO.succeed(1),
      r => UIO.succeed(r)
    )
}
