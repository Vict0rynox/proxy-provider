package org.vicotynox.proxy.provider.source

import org.http4s.{Uri, UrlForm}
import org.http4s.blaze.http.{ClientResponse, HttpClient}
import zio.{Task, ZIO}

trait HttpSource extends ProxySource {
  protected def client: HttpClient

  override val proxySource: ProxySource.Service[Any] =
    (uri: String) => ZIO.fromFuture[String](_ => client.GET(uri) {
      resp => ClientResponse.stringBody(resp)
    })
}
