package org.daron

import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{Resource, Sync}

import scala.concurrent.ExecutionContext

/**
  * Utility object with useful method to create ec as a Resource
  */
object Pools {

  def fixedThreadPool[F[_] : Sync](size: Int): Resource[F, ExecutionContext] = {
    val alloc = Sync[F].delay(Executors.newFixedThreadPool(size))
    val free = (es: ExecutorService) => Sync[F].delay(es.shutdown())
    Resource.make(alloc)(free).map(ExecutionContext.fromExecutor)
  }

}
