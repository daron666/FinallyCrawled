package org.daron.html

import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.softwaremill.sttp.{SttpBackend, _}

trait HttpClient[F[_]] {
  def getPageSource(uri: java.net.URI): F[String]
}

object HttpClient {

  def apply[F[_] : cats.MonadError[?[_], Throwable]](implicit sttpBackend: SttpBackend[F, Nothing]): HttpClient[F] =
    uri => for {
      resp <- sttp.get(Uri(uri)).send()
      result <- resp.body.leftMap(new IllegalStateException(_): Throwable).raiseOrPure
    } yield result
}
