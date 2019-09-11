package org.vicotynox.proxy.provider

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


final case class ProxyId(value: Long) extends AnyVal

final case class ProxyPayload(host: String, port: Int, country: Option[String] = None, level: Option[Int] = None, rating: Int = 0)

final case class Proxy(id: ProxyId, payload: ProxyPayload) {
  def update(from: ProxyItemPatchForm): Proxy =
    this.copy(id = this.id, payload = payload.copy(
      host = from.host.getOrElse(payload.host),
      port = from.port.getOrElse(payload.port),
      country = from.country.orElse(payload.country),
      level = from.level.orElse(payload.level),
      rating = from.level.getOrElse(payload.rating)
    ))
}


final case class ProxyItemPostForm(host: String, port: Int, country: Option[String] = None, level: Option[Int] = None)

final case class ProxyItemPatchForm(host: Option[String], port: Option[Int], country: Option[String] = None, level: Option[Int] = None, rating: Int = 0)
