package org.daron.graphql

import cats.effect.{Effect, IO}
import io.circe.Json
import org.daron.services.HeadlinesService
import sangria.ast.Document
import sangria.execution.{ExceptionHandler, Executor, HandledException}

import scala.concurrent.ExecutionContext

trait GraphQL[F[_]] {
  def executeGraphQl(query: Document,
                     operationName: Option[String],
                     variables: Json): F[Json]
}

object GraphQL {

  def apply[F[_] : Effect](service: HeadlinesService[F], executionContext: ExecutionContext): GraphQL[F] = new DefaultGraphQL[F](service, executionContext)

  private class DefaultGraphQL[F[_] : Effect](service: HeadlinesService[F], executionContext: ExecutionContext) extends GraphQL[F] {

    private val schema = Schema.schema[F]

    override def executeGraphQl(query: Document, operationName: Option[String], variables: Json): F[Json] = {
      import sangria.marshalling.circe._

      implicit val ec = executionContext

      IO.fromFuture {
        IO.apply(
          Executor.execute(
            schema,
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

}
