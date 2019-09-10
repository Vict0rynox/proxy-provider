package org.vicotynox.proxy.provider.http

import org.http4s.{Method, Status}
import org.vicotynox.proxy.provider.{HTTPSpec, TodoId, TodoItem, TodoItemPatchForm, TodoItemPostForm}
import org.vicotynox.proxy.provider.repository.TodoRepository
import org.vicotynox.proxy.provider.repository.TodoRepository.InMemoryTodoRepository
import zio.{DefaultRuntime, Ref, UIO, ZIO}
import zio.interop.catz._
import io.circe.generic.auto._
import org.vicotynox.proxy.provider.http.TodoService.TodoItemWithUri

class TodoServiceSpec extends HTTPSpec {

  import TodoServiceSpec._
  import TodoServiceSpec.todoService._
  import org.http4s.implicits._

  private val app = todoService.service.orNotFound

  describe("TodoService") {
    it("should create new todo items") {
      val req = request(Method.POST, "/").withEntity(TodoItemPostForm("Test"))
      runWithEnv(
        check(
          app.run(req),
          Status.Created,
          Some(TodoItemWithUri(1L, "/1", "Test", completed = false, None))
        )
      )
    }

    it("Should list all items") {
      val initReq = request[TodoTask](Method.POST, "/").withEntity(TodoItemPostForm("Test"))
      val req = request[TodoTask](Method.GET, "/")
      runWithEnv(
        check[TodoTask, List[TodoItemWithUri]](
          app.run(initReq) *> app.run(initReq) *> app.run(req),
          Status.Ok,
          Some(List(
            TodoItemWithUri(1L, "/1", "Test", completed = false, None),
            TodoItemWithUri(2L, "/2", "Test", completed = false, None)
          ))
        )
      )
    }

    it("Should update exist item") {
      val initReq = request[TodoTask](Method.POST, "/").withEntity(TodoItemPostForm("Test"))
      val req = request[TodoTask](Method.PATCH, "/1").withEntity(TodoItemPatchForm(Some("Test1"), Some(true), Some(1)))
      runWithEnv(
        check[TodoTask, TodoItemWithUri](
          app.run(initReq) *> app.run(req),
          Status.Ok,
          Some(
            TodoItemWithUri(1L, "/1", "Test1", completed = true, Some(1)),
          )
        )
      )
    }
    it("Should update not exist item") {
      val req = request[TodoTask](Method.PATCH, "/1").withEntity(TodoItemPatchForm(Some("Test1")))
      runWithEnv(
        check[TodoTask, TodoItemWithUri](
          app.run(req),
          Status.NotFound,
          None
        )
      )
    }
    it("Delete one exist item") {
      val initReq = request[TodoTask](Method.POST, "/").withEntity(TodoItemPostForm("Test"))
      val req = request[TodoTask](Method.DELETE, "/1")
      runWithEnv(
        check[TodoTask, TodoItemWithUri](
          app.run(initReq) *> app.run(req),
          Status.Ok,
          None
        )
      )
    }

    it("Delete one not exist item") {
      val req = request[TodoTask](Method.DELETE, "/1")
      runWithEnv(
        check[TodoTask, TodoItemWithUri](
          app.run(req),
          Status.NotFound,
          None
        )
      )
    }

    it("Delete all item") {
      val initReq = request[TodoTask](Method.POST, "/").withEntity(TodoItemPostForm("Test"))
      val req = request[TodoTask](Method.DELETE, "/")
      runWithEnv(
        check[TodoTask, TodoItemWithUri](
          app.run(initReq) *> app.run(initReq) *> app.run(req),
          Status.Ok,
          None
        )
      )
    }



  }
}

object TodoServiceSpec extends DefaultRuntime {
  val todoService: TodoService[TodoRepository] = TodoService[TodoRepository]("")

  val mkEnv: UIO[TodoRepository] =
    for {
      store <- Ref.make(Map.empty[TodoId, TodoItem])
      counter <- Ref.make(0.toLong)
      repo = InMemoryTodoRepository(store, counter)
      env = new TodoRepository {
        override val todoRepository: TodoRepository.Service[Any] = repo
      }
    } yield env

  def runWithEnv[E, A](task: ZIO[TodoRepository, E, A]): A =
    unsafeRun[E, A](mkEnv.flatMap(env => task.provide(env)))
}