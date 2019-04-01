package org.daron.graphql

import cats.effect._
import cats.effect.implicits._
import org.daron.services.HeadlinesService
import sangria.schema._

/**
  * Schema here is abstracted over F[_], means that all fields need to be def not vals.
  * We can easily wrap all this schema in class not object, and make it val.
  */
object Schema {

  def queries[F[_] : Effect]: ObjectType[HeadlinesService[F], Unit] = ObjectType(
    name = "Query",
    fields = fields(
      Field(
        name = "HeadlinesQuery",
        fieldType = ListType(StringType),
        arguments = List(InputURIArgument, InputHeaderArgument),
        description = Some("All headline values from requested URI"),
        resolve = c => c.ctx.getList(c.arg(InputURIArgument), c.arg(InputHeaderArgument)).toIO.unsafeToFuture()
      )
    )
  )

  def schema[F[_]: Effect] = new Schema(queries)

}
