object ZioBackground {

  sealed trait Console[+A]

  final case class Return[A](value: () => A) extends Console[A]

  final case class ReadLine[A](rest: String => Console[A]) extends Console[A]

  final case class PrintLine[A](line: String, rest: Console[A]) extends Console[A]

  @scala.annotation.tailrec
  def interpret[A](program: Console[A]): A = program match {
    case Return(value) => value()
    case ReadLine(next) =>
      interpret(next(scala.io.StdIn.readLine()))
    case PrintLine(line, next) =>
      println(line)
      interpret(next)
  }

  def success[A](a: => A): Console[A] = Return(() => a)

  def printLine(line: String): Console[Unit] =
    PrintLine(line, success())

  def readLine: Console[String] = ReadLine(line => success(line))

  implicit class ConsoleSyntax[+A](self: Console[A]) {
    def map[B](f: A => B): Console[B] = flatMap(a => success(f(a)))

    def flatMap[B](f: A => Console[B]): Console[B] = self match {
      case Return(value) => f(value())
      case ReadLine(next) => ReadLine(line => next(line).flatMap(f))
      case PrintLine(line, next) => PrintLine(line, next.flatMap(f))
    }
  }

  val example1: Console[Unit] =
    PrintLine("What is your name ?",
      ReadLine(name =>
        PrintLine("Hello, " + name, Return(() => ()))))

  //Flat map syntax
  val example2: Console[String] =
    printLine("What is your name").flatMap {
      _ =>
        readLine.flatMap {
          name: String =>
            printLine("Hello, " + name).map {
              _ => name
            }
        }
    }

  //Short flatMap syntax with for-comprehension
  val example3: Console[String] = for {
    _ <- printLine("What is your name ?")
    name <- readLine
    _ <- printLine("Hello, " + name)
  } yield name


  def main(args: Array[String]): Unit = {
    println(interpret(example3))
  }
}