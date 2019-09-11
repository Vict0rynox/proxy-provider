package org.vicotynox.proxy.provider.repository

import org.vicotynox.proxy.provider.{ProxyId, ProxyItemPatchForm, ProxyItemPostForm}
import zio.ZIO

trait ProxyRepository extends Serializable {
  val proxyRepository: ProxyRepository.Service[Any]
}

object ProxyRepository extends Serializable {

  trait Service[R] extends Serializable {
    def getById(id: ProxyId): ZIO[R, Nothing, Proxy]

    //??? maybe in proxy manager
    def random(): ZIO[R, Nothing, Proxy]

    def create(proxyForm: ProxyItemPostForm): ZIO[R, Nothing, Proxy]

    def update(id: ProxyId, proxyForm: ProxyItemPatchForm): ZIO[R, Nothing, Proxy]

    def delete(id: ProxyId): ZIO[R, Nothing, Unit]
  }

}
