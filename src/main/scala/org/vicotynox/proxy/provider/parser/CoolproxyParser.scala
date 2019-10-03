package org.vicotynox.proxy.provider.parser

import org.vicotynox.proxy.provider.ProxyPayload
import zio.{UIO, ZIO}

trait CoolproxyParser extends Parser {
  override val parser: Parser.Service[Any] = new Parser.Service[Any] {

    override def parse(line: String): UIO[Option[ProxyPayload]] =
      ZIO.succeed(CoolproxyParser.parse(line))
  }
}

object CoolproxyParser {
  private val proxy = """([\d]{1,3}.[\d]{1,3}.[\d]{1,3}.[\d]{1,3}):([\d]+)""".r

  def parse(line: String): Option[ProxyPayload] = line match {
    case proxy(ip, port) => Some(ProxyPayload(ip, port.toInt))
    case _ => None
  }
}