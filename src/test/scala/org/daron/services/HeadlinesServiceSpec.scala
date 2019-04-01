package org.daron.services

import java.net.URI

import cats.effect.IO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.daron.html.HttpHeadlineParser.{H1, H2}
import org.daron.html.{HttpClient, HttpHeadlineParser}
import org.scalatest.{FlatSpec, Matchers}

class HeadlinesServiceSpec extends FlatSpec with Matchers {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs = IO.contextShift(ec)
  implicit val logger = Slf4jLogger.getLogger[IO]

  val client = new HttpClient[IO] {

    override def getPageSource(uri: URI): IO[String] = IO.apply {
      val html =
        """
          |<html>
          |<body>
          |<h1>HEY1</h1>
          |<h1>HEY11</h1>
          |<h1>HEY111</h1>
          |<h1>HEY1111</h1>
          |<h2>HEY2</h2>
          |<h3>HEY2</h3>
          |</body>
          |</html>
        """.stripMargin

      html
    }
  }

  val parser = HttpHeadlineParser[IO]
  val service = HeadlinesService(client, parser, scala.concurrent.ExecutionContext.Implicits.global)

  val uri = new java.net.URI("ya.ru")

  it should "return heedful headlines if everything is ok" in {
    service.getList(uri, H1).unsafeRunSync() should contain theSameElementsAs Seq("HEY1", "HEY11", "HEY111", "HEY1111")
    service.getList(uri, H2).unsafeRunSync() should contain theSameElementsAs Seq("HEY2")
  }

}
