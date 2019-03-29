package org.daron.html

import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import scala.collection.JavaConverters._
import scala.util.Try

trait HttpHeadlineParser[F[_]] {
  def parseHeadlines(input: String): F[List[String]]
}

object HttpHeadlineParser {

  def apply[F[_] : cats.MonadError[?[_], Throwable]]: HttpHeadlineParser[F] = new JsoupHeadlineParser[F]

  private class JsoupHeadlineParser[F[_] : cats.MonadError[?[_], Throwable]] extends HttpHeadlineParser[F] {

    override def parseHeadlines(input: String): F[List[String]] = for {
      doc <- parseAsHtml(input)
      elements <- getH2Elements(doc)
      list <- foldElementsToList(elements)
    } yield list


    private def parseAsHtml(input: String): F[Document] = Try(Jsoup.parse(input)).toEither.raiseOrPure

    private def getH2Elements(doc: Document): F[Elements] = doc.select("h2").pure

    private def foldElementsToList(elements: Elements): F[List[String]] = elements.eachText().asScala.toList.pure
  }

}
