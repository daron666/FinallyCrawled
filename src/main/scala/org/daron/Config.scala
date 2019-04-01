package org.daron

final case class Config(http: Http, executor: BlockingExecutor)

final case class Http(port: Int = 8080, host: String = "localhost")

final case class BlockingExecutor(threads: Int = 5)
