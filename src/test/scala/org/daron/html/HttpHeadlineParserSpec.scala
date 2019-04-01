package org.daron.html

import cats.effect.SyncIO
import org.daron.html.HttpHeadlineParser.{H1, H2}
import org.scalatest.{EitherValues, FlatSpec, Matchers}

class HttpHeadlineParserSpec extends FlatSpec with Matchers with EitherValues {


  val parser = HttpHeadlineParser.apply[SyncIO]

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

  val notAnHtml = "nope nope nope"

  it should "parse only needful headers" in {
    parser.parseHeadlines(html, H1).unsafeRunSync() should contain theSameElementsAs Seq("HEY1", "HEY11", "HEY111", "HEY1111")
    parser.parseHeadlines(html, H2).unsafeRunSync() should contain theSameElementsAs Seq("HEY2")
  }

  it should "return empty sequence if input so not contain needful tags" in {
    parser.parseHeadlines(notAnHtml, H1).unsafeRunSync() should be(empty)
  }

  it should "return an error if something went wrong" in {
    parser.parseHeadlines(null, H1).attempt.unsafeRunSync().left.value shouldBe a[NullPointerException]
  }
}
