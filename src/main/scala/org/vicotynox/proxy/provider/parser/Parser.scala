package org.vicotynox.proxy.provider.parser

import org.vicotynox.proxy.provider.ProxyPayload
import zio.ZIO

trait Parser extends Serializable {
  val parser: Parser.Service[Any]
}

object Parser extends Serializable {

  trait Service[R] extends Serializable {
    def parse(line: String): ZIO[R, Nothing, Option[ProxyPayload]]
  }

}
