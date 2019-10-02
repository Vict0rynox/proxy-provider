package org.vicotynox.proxy.provider.source

import zio.{Task, ZIO}
import zio.stream.{Stream, ZStream}

trait ProxySource extends Serializable {
  val proxySource: ProxySource.Service[Any]
}

object ProxySource extends Serializable {

  trait Service[R] extends Serializable {
    def load(string: String): ZIO[R, Throwable, String]
  }

}
