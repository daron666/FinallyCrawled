package org.daron

import java.net.URI

import cats.syntax.either._
import org.daron.html.HttpHeadlineParser.{H1, H2, HeaderTag}
import sangria.ast
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput, ResultMarshaller}
import sangria.schema.{Argument, EnumType, EnumValue, ScalarType}
import sangria.validation.ValueCoercionViolation

import scala.util.Try

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
}
