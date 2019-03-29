import java.net.URI

import cats.effect.{ExitCode, IO, IOApp}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.daron.html.{HttpClient, HttpHeadlineParser}

object HelloWorld extends IOApp {

  implicit val backend = AsyncHttpClientCatsBackend[IO]()

  override def run(args: List[String]): IO[ExitCode] = for {
    logger <- Slf4jLogger.create[IO]
    _ <- logger.info("I'm Alive! Hello World")
    res <- HttpClient.apply[IO].getPageSource(new URI("nytimes.com"))
    headers <- HttpHeadlineParser.apply[IO].parseHeadlines(res)
    _ <- logger.info(s"Headers: $headers")
    _ <- IO.apply(backend.close())
  } yield ExitCode.Success

}
