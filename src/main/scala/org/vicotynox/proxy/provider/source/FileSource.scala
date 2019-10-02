package org.vicotynox.proxy.provider.source

import java.io.IOException

import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import zio.stream.{Stream, ZStream}
import zio.{IO, Task, UIO, ZIO}

import scala.io.Source
import scala.util.Try


trait FileSource extends ProxySource {

  override val proxySource: ProxySource.Service[Any] = new ProxySource.Service[Any] {

    private def closeSource(source: Source) = UIO(source.close())

    override def load(path: String): Task[String] =
      Task(Source.fromResource(path)).bracket(closeSource)(s => ZIO.fromTry(Try(s.mkString)))

  }
}
