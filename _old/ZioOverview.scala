import java.io.IOException

import zio.{App, Fiber, Task, UIO, ZIO}
import zio.console._
import zio.duration._

import scala.concurrent.Future
import scala.util.Try

object ZioOverview extends App {
  override def run(args: List[String]): ZIO[ZioOverview.Environment, Nothing, Int] =
    myAppLogic.fold(_ => 1, _ => 0)

  val myAppLogic: ZIO[ZioOverview.Environment, Throwable, Unit] =
    for {
      _ <- putStrLn("Hello! What is your name ?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
      v1 <- s1
      v2 <- s2
      v3 <- zfun.provide(5)
      _ <- putStrLn(s"${v1}\n${v2}\n${v3}")
      fib <- fib100Fiber
      _ <- putStrLn(s"Fib: ${fib}")
      res <- fib.join.timeout(30.seconds)
      //res <- fib.interrupt
      _ <- putStrLn(s"Result: ${res}")

    } yield ()


  //not lazy (eager)
  private val s1 = ZIO.succeed(42)
  private val s2: Task[Int] = Task.succeed(42)

  private lazy val bigList = (0 to 10000000).toList
  private lazy val bigString = bigList.map(_.toString).mkString("\n")

  //lazy
  private val s3 = ZIO.effectTotal(bigString)

  private val f1 = ZIO.fail("Oh oh!")

  private val f2 = Task.fail(new Exception("Oh oh!"))

  private val zoption: ZIO[Any, Unit, Int] = ZIO.fromOption(Some(2))

  private val zoption2: ZIO[Any, String, Int] = zoption.mapError(_ => "")

  private val zeither = ZIO.fromEither(Right("Success!"))

  private val ztry = ZIO.fromTry(Try(42 / 0))

  private val zfun: ZIO[Int, Nothing, Int] = ZIO.fromFunction((i: Int) => i * i)

  private lazy val future = Future.successful("Hello!")

  private val zfuture: Task[String] = ZIO.fromFuture {
    implicit ec => future.map(_ => "Goodbye!")
  }

  def fib(n: Long): UIO[Long] = UIO {
    if (n <= 1) UIO.succeed(n)
    else fib(n - 1).zipWith(fib(n - 2))(_ + _)
  }.flatten

  private val fib100Fiber: UIO[Fiber[Nothing, Long]] =
    for {
      fiber <- fib(100).fork
    } yield fiber

}
