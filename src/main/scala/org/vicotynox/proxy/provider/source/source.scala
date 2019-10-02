package org.vicotynox.proxy.provider

import zio.{RIO, ZIO}
import zio.stream.ZStream
import zio.stream.Stream

package object source extends ProxySource.Service[ProxySource] {
  override def load(string: String): ZIO[ProxySource, Throwable, String] =
    ZIO.accessM(_.proxySource.load(string))
}
