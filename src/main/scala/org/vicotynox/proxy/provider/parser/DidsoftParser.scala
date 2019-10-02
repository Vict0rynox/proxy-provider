package org.vicotynox.proxy.provider.parser

import org.vicotynox.proxy.provider.ProxyPayload
import zio.{UIO, ZIO}

trait DidsoftParser extends Parser {
  override val parser: Parser.Service[Any] = new Parser.Service[Any] {

    override def parse(line: String): UIO[Option[ProxyPayload]] =
      ZIO.succeed(ProxyParser.parse(line))
  }
}

object ProxyParser {
  private val proxy = """([\d]{1,3}.[\d]{1,3}.[\d]{1,3}.[\d]{1,3}):([\d]+)#([A-Za-z]{2})""".r

  def parse(line: String): Option[ProxyPayload] = line match {
    case proxy(ip, port, country) => Some(ProxyPayload(ip, port.toInt, Some(country)))
    case _ => None
  }
}