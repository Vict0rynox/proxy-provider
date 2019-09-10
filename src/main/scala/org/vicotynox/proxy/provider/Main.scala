package org.vicotynox.proxy.provider

import org.http4s.server.Router
import org.vicotynox.proxy.provider.config._
import org.vicotynox.proxy.provider.repository.DoobieTodoRepository
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console._
import zio.interop.catz._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import cats.effect._
import org.vicotynox.proxy.provider.http.TodoService
import org.vicotynox.proxy.provider.repository.TodoRepository
import pureconfig.generic.auto._
import zio._


object Main extends App {

  type AppEnvironment = Clock with Console with Blocking with TodoRepository

  type AppTask[A] = RIO[AppEnvironment, A]

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (for {
      cfg <- ZIO.fromEither(pureconfig.loadConfig[Config])

      _ <- initDb(cfg.dbConfig)

      blockingEC <- ZIO.environment[Blocking].flatMap(_.blocking.blockingExecutor).map(_.asEC)
      transactorR = mkTransactor(cfg.dbConfig, Platform.executor.asEC, blockingEC)
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

      program <- transactorR.use {
        transactor =>
          server.provideSome[Environment] {
            base =>
              new Clock with Console with Blocking with DoobieTodoRepository {
                override protected def xa: doobie.Transactor[Task] = transactor

                override val clock: Clock.Service[Any] = base.clock
                override val console: Console.Service[Any] = base.console
                override val blocking: Blocking.Service[Any] = base.blocking
              }
          }
      }


    } yield program).foldM(err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
}
