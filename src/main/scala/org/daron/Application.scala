package org.daron

import java.net.URI
import java.util.concurrent.{ExecutorService, Executors}

import cats.effect._
import cats.syntax.functor._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import org.daron.html.HttpHeadlineParser.H2
import org.daron.html.{HttpClient, HttpHeadlineParser}
import org.daron.services.HeadlinesService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext

object Application extends IOApp {

  //TODO: Should be config
  private val port: Int = 8080
  private val host: String = "localhost"
  private val blockingPoolSize: Int = 5



  def sttpBackend[F[_] : Async]: Resource[F, SttpBackend[F, Nothing]] = {
    val alloc = Sync[F].delay(AsyncHttpClientCatsBackend[F]())
    val free = (bc: SttpBackend[F, Nothing]) => Sync[F].delay {
      println("CLOSING BC")
      bc.close()
    }
    Resource.make(alloc)(free)
  }

  def httpRoutes[F[_] : Sync](service: HeadlinesService[F])(implicit dsl: Http4sDsl[F]) = {
    import dsl._

    //TODO: Arguments could be read from incoming request
    HttpRoutes.of[F] {
      case GET -> Root / "rest" => Ok(service.getList(new URI("nytimes.com"), H2).map(_.asJson))
    }
  }

  def start[F[_] : ConcurrentEffect : ContextShift : Timer](routes: HttpRoutes[F]): Resource[F, Server[F]] = {
    BlazeServerBuilder[F]
      .bindHttp(port, host)
      .withHttpApp(routes.orNotFound)
      .resource
  }

  def server[F[_] : ConcurrentEffect : ContextShift : Timer](implicit logger: Logger[F]): Resource[F, Server[F]] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}


    val resources = for {
      bc <- sttpBackend
      ec <- Pools.fixedThreadPool(blockingPoolSize)
    } yield (bc, ec)


    resources.flatMap { case (bc, ec) =>
      implicit val backend = bc
      val client = HttpClient[F]
      val parser = HttpHeadlineParser[F]
      val service = HeadlinesService[F](client, parser, ec)
      val routes = httpRoutes(service)
      start(routes)
    }
  }

  def program[F[_] : ConcurrentEffect : ContextShift : Timer] = {
    implicit val logger = Slf4jLogger.getLogger[F]
    server[F]
  }

  override def run(args: List[String]): IO[ExitCode] = program[IO].use(_ => IO.never.as(ExitCode.Success))
}
