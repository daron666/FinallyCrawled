package org.daron

import java.net.URI

import cats.effect._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import io.circe.jawn._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import org.daron.graphql._
import org.daron.html.HttpHeadlineParser.H2
import org.daron.html.{HttpClient, HttpHeadlineParser}
import org.daron.services.HeadlinesService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze._
import sangria.parser.QueryParser

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Application extends IOApp {

  //TODO: Should be config
  private val port: Int = 8080
  private val host: String = "localhost"
  private val blockingPoolSize: Int = 5

  def sttpBackend[F[_] : Async]: Resource[F, SttpBackend[F, Nothing]] = {
    val alloc = Sync[F].delay(AsyncHttpClientCatsBackend[F]())
    val free = (bc: SttpBackend[F, Nothing]) => Sync[F].delay(bc.close())
    Resource.make(alloc)(free)
  }

  /**
    * Most likely this method should be moved in a separate file/module.
    * @param service
    * @param ec
    * @param dsl
    * @tparam F
    * @return
    */
  def httpRoutes[F[_] : Effect : ContextShift](service: HeadlinesService[F],
                                               ec: ExecutionContext)(implicit dsl: Http4sDsl[F]) = {
    import dsl._

    HttpRoutes.of[F] {
      case request@GET -> Root =>
        StaticFile.fromResource("/assets/playground.html", ec, Some(request)).getOrElseF(NotFound())
      case request@POST -> Root / "graphql" =>
        request.as[Json].flatMap { body =>
          val query = root.query.string.getOption(body)
          val operationName = root.operationName.string.getOption(body)
          val variablesStr = root.variables.string.getOption(body)

          query.map(QueryParser.parse(_)) match {
            case Some(Success(ast)) =>
              variablesStr.map(parse) match {
                case Some(Left(error)) => BadRequest(formatError(error).pure)
                case Some(Right(json)) => Ok(executeGraphQl(ast, operationName, json, service, ec))
                case None => Ok(executeGraphQl(ast, operationName, root.variables.json.getOption(body) getOrElse Json.obj(), service, ec))
              }
            case Some(Failure(error)) => BadRequest(formatError(error).pure)
            case None => BadRequest(formatError("No query to execute").pure)
          }
        }
      case GET -> Root / "rest" =>
        Ok(service.getList(new URI("nytimes.com"), H2).map(_.asJson))
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


    /**
      * We are using the same blocking EC for everything. Should use different ec for different services.
      * But for test sake it makes sense not to worry about it right now.
      * Also init all the resources that should be free on exit here.
      */
    val resources = for {
      bc <- sttpBackend
      ec <- Pools.fixedThreadPool(blockingPoolSize)
    } yield (bc, ec)

    resources.flatMap { case (bc, ec) =>
      implicit val backend = bc
      val client = HttpClient[F]
      val parser = HttpHeadlineParser[F]
      val service = HeadlinesService[F](client, parser, ec)
      val routes = httpRoutes(service, ec)
      start(routes)
    }
  }

  def program[F[_] : ConcurrentEffect : ContextShift : Timer] = {
    implicit val logger = Slf4jLogger.getLogger[F]
    server[F]
  }

  override def run(args: List[String]): IO[ExitCode] = program[IO].use(_ => IO.never.as(ExitCode.Success))
}
