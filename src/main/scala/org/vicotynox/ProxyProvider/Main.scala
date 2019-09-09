package org.vicotynox.ProxyProvider

import org.http4s.server.Router
import org.vicotynox.ProxyProvider.config._
import org.vicotynox.ProxyProvider.http.TodoService
import org.vicotynox.ProxyProvider.repository.TodoRepository
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console._
import zio.interop.catz._
import fs2.Stream.Compiler._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import cats.effect._
import org.vicotynox.ProxyProvider.repository.TodoRepository.InMemoryTodoRepository
import pureconfig.generic.auto._


object Main extends App {

  type AppEnvironment = Clock with Console with Blocking with TodoRepository

  type AppTask[A] = RIO[AppEnvironment, A]

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (for {
      cfg <- ZIO.fromEither(pureconfig.loadConfig[Config])
      //blockingEC <- ZIO.environment[Blocking].flatMap(_.blocking.blockingExecutor).map(_.asEC)
      httpApp = Router[AppTask](
        "/todos" -> TodoService(s"${cfg.appConfig.baseUrl}/todos").service
      ).orNotFound

      server = ZIO.runtime[AppEnvironment].flatMap {
        implicit rts =>
          BlazeServerBuilder[AppTask]
            .bindHttp(cfg.appConfig.port, cfg.appConfig.endpoint)
            .withHttpApp(CORS(httpApp))
            .serve
            .compile[AppTask, AppTask, ExitCode]
            .drain
      }

      storage <- Ref.make(Map.empty[TodoId, TodoItem])
      counter <- Ref.make(0.toLong)
      program <- server.provideSome[Environment] {
        base =>
          new Clock with Console with Blocking with TodoRepository {
            override val clock: Clock.Service[Any] = base.clock
            override val console: Console.Service[Any] = base.console
            override val blocking: Blocking.Service[Any] = base.blocking
            override val todoRepository: TodoRepository.Service[Any] = InMemoryTodoRepository(storage, counter)
          }
      }

    } yield program).foldM(err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
}
