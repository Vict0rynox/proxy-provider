package org.vicotynox.ProxyProvider

import doobie.hikari._
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import zio._
import zio.interop.catz._

import scala.concurrent.ExecutionContext

object config {

  final case class Config(appConfig: AppConfig, dbConfig: DbConfig)

  final case class AppConfig(endpoint: String, baseUrl: String, port: Int)

  final case class DbConfig(url: String, driver: String, user: String, password: String)


  def initDb(cfg: DbConfig): Task[Unit] =
    ZIO.effect {
      val fw = Flyway
        .configure()
        .dataSource(cfg.url, cfg.user, cfg.password)
        .load()
      fw.migrate()
    }.unit

  def mkTransactor(cfg: DbConfig, connectEC: ExecutionContext, transactEC: ExecutionContext): Managed[Throwable, Transactor[Task]]
  = {
    val xa = HikariTransactor.newHikariTransactor[Task](
      cfg.driver,
      cfg.url,
      cfg.user,
      cfg.password,
      connectEC,
      transactEC)

    val res = xa.allocated.map {
      case(transactor, cleanupM) => Reservation(ZIO.succeed(transactor), _ => cleanupM.orDie)
    }.uninterruptible

    Managed(res)
  }
}