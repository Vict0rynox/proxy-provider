package org.vicotynox.proxy.provider

import zio.ZIO

package object repository extends TodoRepository.Service[TodoRepository] {
  override def getAll: ZIO[TodoRepository, Nothing, List[TodoItem]] =
    ZIO.accessM(_.todoRepository.getAll)

  override def getById(id: TodoId): ZIO[TodoRepository, Nothing, Option[TodoItem]] =
    ZIO.accessM(_.todoRepository.getById(id))

  override def delete(id: TodoId): ZIO[TodoRepository, Nothing, Unit] =
    ZIO.accessM(_.todoRepository.delete(id))

  override def deleteAll(): ZIO[TodoRepository, Nothing, Unit] =
    ZIO.accessM(_.todoRepository.deleteAll())

  override def create(todoItemForm: TodoItemPostForm): ZIO[TodoRepository, Nothing, TodoItem] =
    ZIO.accessM(_.todoRepository.create(todoItemForm))

  override def update(id: TodoId, todoItemForm: TodoItemPatchForm): ZIO[TodoRepository, Nothing, Option[TodoItem]] =
    ZIO.accessM(_.todoRepository.update(id, todoItemForm))
}
