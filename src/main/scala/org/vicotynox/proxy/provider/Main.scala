package org.vicotynox.proxy.provider


import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.security.KeyStore.TrustedCertificateEntry

import com.sun.deploy.cache.InMemoryLocalApplicationProperties
import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig, Dsl, RequestBuilder, Response}
import org.asynchttpclient.proxy.ProxyServer
import org.http4s.Status
import org.http4s.blaze.http.HttpClient
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.server.Router
import org.vicotynox.proxy.provider.config._
import org.vicotynox.proxy.provider.repository.{DoobieTodoRepository, ProxyRepository}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{putStrLn, _}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.vicotynox.proxy.provider.parser.{DidsoftParser, Parser}
import org.vicotynox.proxy.provider.repository.ProxyRepository.InMemoryProxyRepository
import org.vicotynox.proxy.provider.source.{FileSource, ProxySource}

import scala.io.Source
import scala.util.Try
import zio.duration._

import scala.concurrent.Future
//import cats.effect._
import org.vicotynox.proxy.provider.http.TodoService
import org.vicotynox.proxy.provider.parser._
import org.vicotynox.proxy.provider.source._
import org.vicotynox.proxy.provider.repository.TodoRepository
import pureconfig.generic.auto._
import zio._
import zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits.global


object Main extends App {


  type AppEnvironment = Clock with Console with Blocking with TodoRepository

  type TestEnvironment = Clock with Console with Blocking with Parser with ProxySource with ProxyRepository

  type AppTask[A] = RIO[AppEnvironment, A]

  /*override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
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

    } yield program).foldM(err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))*/


  def closeStream(source: Source) =
    UIO(source.close())

  def proxys(path: String): ZIO[TestEnvironment, Throwable, List[ProxyPayload]] =
    for {
      string <- load(path)
      list <- ZIO.collectAll[TestEnvironment, Throwable, Option[ProxyPayload]](string.split("\n").map(parse).toList)
      proxies = list.filter(_.fold(false)(_ => true)).map(f => f.get)
    } yield proxies

  def checkProxy(proxyPayload: ProxyPayload): ZIO[TestEnvironment, Nothing, Unit] = {
    val cf = new DefaultAsyncHttpClientConfig.Builder()
      .setProxyServer(new ProxyServer.Builder(proxyPayload.host, proxyPayload.port))
      .build

    val c = new DefaultAsyncHttpClient(cf)
    val rb =
      new RequestBuilder("GET").setUrl("https://ir.ebaystatic.com/rs/v/fxxj3ttftm5ltcqnto1o4baovyl.png")

    val response: Try[Response] = Try(c.prepareRequest(rb).execute.get)
    if (response.isSuccess && response.get.getStatusCode == 200) {
      val proxy = Proxy(proxyPayload)
      ZIO.accessM[TestEnvironment](_.proxyRepository.create(proxy))
      putStrLn(s"Save proxy: $proxy")
    } else putStrLn(s"proxy not valid ${proxyPayload}\t${response}")
  }

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] = (
    for {
      _ <- putStrLn("Start program ")

      store <- Ref.make(Map.empty[ProxyId, Proxy])
      program <- ZIO.runtime[TestEnvironment].provideSome[Environment] {
        base =>
          new Clock with Console with Blocking with FileSource with CoolproxyParser with ProxyRepository {
            override val clock: Clock.Service[Any] = base.clock
            override val console: Console.Service[Any] = base.console
            override val blocking: Blocking.Service[Any] = base.blocking
            override val proxyRepository: ProxyRepository.Service[Any] = InMemoryProxyRepository(store)
          }
      }

      proxyQueue <- Queue.sliding[ProxyPayload](100)
      loaderTicker = Schedule.spaced(5.second) && Schedule.recurs(1)
      handleTicker = Schedule.spaced(1.second) && Schedule.recurs(300)

      sendTask <- (for {
        proxies <- proxys("proxy.dat").provide(program.Environment)
        _ <- ZIO.collectAll(proxies.map(proxyQueue.offer))
        _ <- putStrLn(s"Send: $proxies")
      } yield ()).repeat(loaderTicker).fork

      handle1 <- (for {
        proxyPayload <- proxyQueue.take
        _ <- putStrLn(s"Take: $proxyPayload")
        _ <- checkProxy(proxyPayload).provide(program.Environment)
      } yield ()).repeat(handleTicker).fork

      handle2 <- (for {
        proxyPayload <- proxyQueue.take
        _ <- putStrLn(s"Take: $proxyPayload")
        _ <- checkProxy(proxyPayload).provide(program.Environment)
      } yield ()).repeat(handleTicker).fork

      allTasks = sendTask.zip(handle1).zip(handle2)

      _ <- allTasks.join
      _ <- putStrLn(s"${store.get}")
    } yield program).foldM(err => putStrLn(s"Exceution faild with $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
}
