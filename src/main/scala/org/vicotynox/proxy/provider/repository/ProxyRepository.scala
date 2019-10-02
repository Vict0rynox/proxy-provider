package org.vicotynox.proxy.provider.repository

import org.vicotynox.proxy.provider.{Proxy, ProxyId, ProxyItemPatchForm, ProxyItemPostForm}
import zio.{Ref, Task, UIO, ZIO}

trait ProxyRepository extends Serializable {
  val proxyRepository: ProxyRepository.Service[Any]
}

object ProxyRepository extends Serializable {

  trait Service[R] extends Serializable {
    def getById(id: ProxyId): ZIO[R, Nothing, Option[Proxy]]

    def create(proxyForm: ProxyItemPostForm): ZIO[R, Nothing, Proxy]
    def create(proxy: Proxy): ZIO[R, Nothing, Proxy]

    def update(id: ProxyId, proxyForm: ProxyItemPatchForm): ZIO[R, Nothing, Option[Proxy]]

    def delete(id: ProxyId): ZIO[R, Nothing, Unit]
  }

  final case class InMemoryProxyRepository(ref: Ref[Map[ProxyId, Proxy]]) extends Service[Any] {

    override def getById(id: ProxyId): ZIO[Any, Nothing, Option[Proxy]] =
      ref.get.map(_.get(id))

    override def create(proxyForm: ProxyItemPostForm): ZIO[Any, Nothing, Proxy] =
      for {
        proxy <- ZIO.succeed(proxyForm.asProxy)
        hasProxy <- getById(proxy.id)
        //FIXME: maybe need return error if exist
        _ <- hasProxy.fold(ref.update(store => store + (proxy.id -> proxy)) *> ZIO.succeed(proxy))(ZIO.succeed)
      } yield proxy

    override def create(proxy: Proxy): ZIO[Any, Nothing, Proxy] =
      for {
        hasProxy <- getById(proxy.id)
        //FIXME: maybe need return error if exist
        _ <- hasProxy.fold(ref.update(store => store + (proxy.id -> proxy)) *> ZIO.succeed(proxy))(ZIO.succeed)
      } yield proxy

    override def update(id: ProxyId, proxyForm: ProxyItemPatchForm): ZIO[Any, Nothing, Option[Proxy]] =
      for {
        proxy <- getById(id)
        res <- proxy.fold[UIO[Option[Proxy]]](ZIO.succeed(None)) {
          proxy =>
            val newProxy = proxy.update(proxyForm)
            ref.update(store => store + (proxy.id -> newProxy)) *> ZIO.succeed(Some(newProxy))
        }
      } yield res

    override def delete(id: ProxyId): ZIO[Any, Nothing, Unit] =
      ref.update(store => store - id).unit
  }

}
