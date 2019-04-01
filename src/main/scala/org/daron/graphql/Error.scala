package org.daron.graphql

import io.circe.Json
import sangria.parser.SyntaxError

import scala.util.control.NonFatal

object Error {

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
}
