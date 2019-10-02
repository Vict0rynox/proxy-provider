package org.vicotynox.proxy.provider

import java.security.MessageDigest

final case class TodoId(value: Long) extends AnyVal

final case class TodoPayload(title: String, completed: Boolean, order: Option[Int])

final case class TodoItem(id: TodoId, item: TodoPayload) {
  def update(form: TodoItemPatchForm): TodoItem = {
    this.copy(id = this.id,
      item = item.copy(
        title = form.title.getOrElse(item.title),
        completed = form.completed.getOrElse(item.completed),
        order = form.order.orElse(item.order)
      )
    )
  }
}

final case class TodoItemPostForm(title: String, order: Option[Int] = None) {
  def asTodoItem(id: TodoId): TodoItem = TodoItem(id, this.asTodoPayload)

  def asTodoPayload: TodoPayload = TodoPayload(title, completed = false, order)
}

final case class TodoItemPatchForm(
                                    title: Option[String] = None,
                                    completed: Option[Boolean] = None,
                                    order: Option[Int] = None
                                  )

//Proxy

final case class ProxyId(value: String) extends AnyVal

object ProxyId {
  def apply(payload: ProxyPayload): ProxyId =
    new ProxyId(MessageDigest.getInstance("MD5")
      .digest(s"${payload.host}:${payload.port}".getBytes()).map(_.toChar).mkString)
}

final case class ProxyPayload(host: String, port: Int, country: Option[String] = None, level: Option[Int] = None, rating: Int = 0)

final case class Proxy(payload: ProxyPayload) {
  val id: ProxyId = ProxyId(payload)

  def update(from: ProxyItemPatchForm): Proxy =
    this.copy(payload = payload.copy(
      country = from.country.orElse(payload.country),
      level = from.level.orElse(payload.level),
      rating = from.level.getOrElse(payload.rating)
    ))
}


final case class ProxyItemPostForm(host: String, port: Int, country: Option[String] = None, level: Option[Int] = None) {
  def asProxyPayload: ProxyPayload = ProxyPayload(host, port, country, level)

  def asProxy = Proxy(this.asProxyPayload)
}

final case class ProxyItemPatchForm(country: Option[String] = None, level: Option[Int] = None, rating: Int = 0)
