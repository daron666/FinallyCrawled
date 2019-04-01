package org.daron.services

import java.net.URI

import cats.Monad
import cats.effect.ContextShift
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import org.daron.html.HttpHeadlineParser.HeaderTag
import org.daron.html.{HttpClient, HttpHeadlineParser}

import scala.concurrent.ExecutionContext

trait HeadlinesService[F[_]] {

  def getList(uri: URI, tag: HeaderTag): F[List[String]]
}

object HeadlinesService {


  def apply[F[_] : ContextShift : Monad : Logger](client: HttpClient[F],
                                                  parser: HttpHeadlineParser[F],
                                                  ec: ExecutionContext): HeadlinesService[F] =
    new DefaultHeadlinesService[F](client, parser, ec)

  private class DefaultHeadlinesService[F[_] : ContextShift : Monad : Logger](httpClient: HttpClient[F],
                                                                              httpHeadlineParser: HttpHeadlineParser[F],
                                                                              ec: ExecutionContext)
    extends HeadlinesService[F] {

    def getList(uri: URI, tag: HeaderTag): F[List[String]] = {
      val io = for {
        string <- httpClient.getPageSource(uri)
        result <- httpHeadlineParser.parseHeadlines(string, tag)
      } yield result
      ContextShift[F].evalOn(ec)(io)
    }

  }

}


