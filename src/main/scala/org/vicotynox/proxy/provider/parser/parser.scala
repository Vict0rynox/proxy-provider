package org.vicotynox.proxy.provider

import zio.ZIO

package object parser extends Parser.Service[Parser] {
  override def parse(line: String): ZIO[Parser, Nothing, Option[ProxyPayload]] =
    ZIO.accessM(_.parser.parse(line))
}
