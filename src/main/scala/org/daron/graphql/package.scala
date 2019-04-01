package org.daron

import java.net.URI

import cats.effect._
import cats.syntax.either._
import io.circe.Json
import org.daron.html.HttpHeadlineParser.{H1, H2, HeaderTag}
import org.daron.services.HeadlinesService
import sangria.ast
import sangria.ast.Document
import sangria.execution.{Executor, _}
import sangria.marshalling.circe._
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput, ResultMarshaller}
import sangria.parser.SyntaxError
import sangria.schema.{Argument, EnumType, EnumValue, ScalarType}
import sangria.validation.ValueCoercionViolation

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.control.NonFatal

/**
  * This package object should be split into meaningful parts.
  */
package object graphql {

  final case object InvalidURI extends ValueCoercionViolation("URI expected")

  val HeaderEnum = EnumType(
    name = "HeaderTag",
    description = Some("Header tag to look. Possible values: H1, H2"),
    values = List(
      EnumValue("H1", value = H1),
      EnumValue("H2", value = H2)
    )
  )

  final object URIType extends ScalarType[java.net.URI](
    name = "URI",
    description = Some("String representation of a java.net.URI"),
    coerceOutput = (uri: java.net.URI, _) => ast.StringValue(uri.toString),
    coerceUserInput = {
      case s: String => Try(new java.net.URI(s)).toEither.leftMap(_ => InvalidURI)
      case _ => Left(InvalidURI)
    },
    coerceInput = {
      case sv: ast.StringValue => Try(new java.net.URI(sv.value)).toEither.leftMap(_ => InvalidURI)
      case _ => Left(InvalidURI)
    })


  implicit val uriFromInput: FromInput[URI] = {
    new FromInput[URI] {
      override val marshaller: ResultMarshaller = CoercedScalaResultMarshaller.default

      override def fromResult(node: marshaller.Node): URI = {
        node match {
          case id: URI => id
          case invalidInput =>
            throw new IllegalArgumentException(s"Couldn't extract type URI from input $invalidInput")
        }
      }
    }
  }

  implicit val headerTagFromInput: FromInput[HeaderTag] = {
    new FromInput[HeaderTag] {
      override val marshaller: ResultMarshaller = CoercedScalaResultMarshaller.default

      override def fromResult(node: marshaller.Node): HeaderTag = {
        node match {
          case tag: HeaderTag => tag
          case invalidInput =>
            throw new IllegalArgumentException(s"Couldn't extract type HeaderTag from input $invalidInput")
        }
      }
    }
  }

  val InputURIArgument: Argument[URI] = Argument[URI](
    name = "uri",
    argumentType = URIType,
    description = "URI to look"
  )

  val InputHeaderArgument: Argument[HeaderTag] = Argument[HeaderTag](
    name = "hTag",
    argumentType = HeaderEnum,
    description = "Header Tag value to look"
  )

  def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError =>
      Json.obj("errors" → Json.arr(
        Json.obj(
          "message" → Json.fromString(syntaxError.getMessage),
          "locations" → Json.arr(Json.obj(
            "line" → Json.fromBigInt(syntaxError.originalError.position.line),
            "column" → Json.fromBigInt(syntaxError.originalError.position.column))))))
    case NonFatal(e) =>
      formatError(e.getMessage)
    case e =>
      throw e
  }

  def formatError(message: String): Json =
    Json.obj("errors" → Json.arr(Json.obj("message" → Json.fromString(message))))


  /**
    * We can use this method, but for more abstraction we can design/implement
    * GraphQL algebra with multiple methods like ```def execute(data): F[Either[Json, Json]]```
    *
    * @param query
    * @param operationName
    * @param variables
    * @param service
    * @param executionContext
    * @tparam F
    * @return
    */
  def executeGraphQl[F[_] : Effect](query: Document,
                                    operationName: Option[String],
                                    variables: Json,
                                    service: HeadlinesService[F],
                                    executionContext: ExecutionContext) = {

    implicit val ec = executionContext

    IO.fromFuture {

      IO.apply(
        Executor.execute(
          Schema.schema,
          query,
          service,
          variables = if (variables.isNull) Json.obj() else variables,
          operationName = operationName,
          exceptionHandler = ExceptionHandler {
            case (_, e) ⇒ HandledException(e.getMessage)
          }
        )
      )
    }.to[F]
  }

}
