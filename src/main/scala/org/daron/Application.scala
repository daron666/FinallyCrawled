package org.daron

import cats.effect._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.daron.html.{HttpClient, HttpHeadlineParser}
import org.daron.services.HeadlinesService
import org.http4s._
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze._

trait Application extends {

  //Don't remove
  import pureconfig.generic.auto._

  /**
    * For now I decided not to control config loading as an effect just for simplicity
    */
  private val config = pureconfig.loadConfigOrThrow[Config]
  private val port: Int = config.http.port
  private val host: String = config.http.host
  private val blockingPoolSize: Int = config.executor.threads

  /**
    * Should be moved where it belongs better. For this current case it's ok to leave it here.
    *
    * @tparam F
    * @return
    */
  def sttpBackend[F[_] : Async]: Resource[F, SttpBackend[F, Nothing]] = {
    val alloc = Sync[F].delay(AsyncHttpClientCatsBackend[F]())
    val free = (bc: SttpBackend[F, Nothing]) => Sync[F].delay(bc.close())
    Resource.make(alloc)(free)
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
      val routes = Routes.httpRoutes(service, ec)
      start(routes)
    }
  }

  def program[F[_] : ConcurrentEffect : ContextShift : Timer] = {
    implicit val logger = Slf4jLogger.getLogger[F]
    server[F]
  }


}
