package org.daron

import java.net.URI

import cats.effect._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.Json
import io.circe.jawn._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import org.daron.graphql._
import org.daron.html.HttpHeadlineParser.H2
import org.daron.services.HeadlinesService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import sangria.parser.QueryParser

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Routes {

  def httpRoutes[F[_] : Effect : ContextShift](service: HeadlinesService[F],
                                               ec: ExecutionContext)(implicit dsl: Http4sDsl[F]) = {
    import dsl._
    import org.daron.graphql.Error._

    val graphQL = GraphQL[F](service, ec)

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
                case Some(Right(json)) => Ok(graphQL.executeGraphQl(ast, operationName, json))
                case None => Ok(graphQL.executeGraphQl(ast, operationName, root.variables.json.getOption(body) getOrElse Json.obj()))
              }
            case Some(Failure(error)) => BadRequest(formatError(error).pure)
            case None => BadRequest(formatError("No query to execute").pure)
          }
        }
      case GET -> Root / "rest" =>
        Ok(service.getList(new URI("nytimes.com"), H2).map(_.asJson))
    }
  }

}
