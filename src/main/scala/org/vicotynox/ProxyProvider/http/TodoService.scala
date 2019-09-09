package org.vicotynox.ProxyProvider.http

import org.vicotynox.ProxyProvider.repository.{update, _}
import org.vicotynox.ProxyProvider._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import zio.RIO
import zio.interop.catz._

final case class TodoService[R <: TodoRepository](rootUri: String) {

  import TodoService._

  type TodoTask[A] = RIO[R, A]

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[TodoTask, A] = jsonOf[TodoTask, A]

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[TodoTask, A] = jsonEncoderOf[TodoTask, A]

  val dsl: Http4sDsl[TodoTask] = Http4sDsl[TodoTask]

  import dsl._

  def service: HttpRoutes[TodoTask] = {
    HttpRoutes.of[TodoTask] {
      case GET -> Root =>
        Ok(getAll.map(_.map(TodoItemWithUri(rootUri, _))))

      case GET -> Root / LongVar(id) =>
        for {
          todo <- getById(TodoId(id))
          response <- todo.fold(NotFound())(x => Ok(TodoItemWithUri(rootUri, x)))
        } yield response

      case req@POST -> Root =>
        req.decode[TodoItemPostForm] {
          todoItemForm => create(todoItemForm).map(TodoItemWithUri(rootUri, _)).flatMap(Created(_))
        }

      case req@PATCH -> Root / LongVar(id) =>
        req.decode[TodoItemPatchForm] {
          todoItemForm =>
            update(TodoId(id), todoItemForm)
              .flatMap(_.fold(NotFound())(x => Ok(TodoItemWithUri(rootUri, x))))
        }

      case DELETE -> Root => deleteAll() *> Ok()

      case DELETE -> Root / LongVar(id) =>
        for {
          item <- getById(TodoId(id))
          result <- item.map(todoItem => delete(todoItem.id)).fold(NotFound())(_.flatMap(Ok(_)))
        } yield result
    }
  }
}

object TodoService {

  final case class TodoItemWithUri(id: Long, url: String, title: String, completed: Boolean, order: Option[Int])

  object TodoItemWithUri {
    def apply(basePath: String, todoItem: TodoItem): TodoItemWithUri =
      new TodoItemWithUri(
        todoItem.id.value,
        s"$basePath/${todoItem.id.value}",
        todoItem.item.title,
        todoItem.item.completed,
        todoItem.item.order
      )

  }

}
