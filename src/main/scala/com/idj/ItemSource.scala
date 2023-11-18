package com.idj

import scala.concurrent.Future

trait ItemSource[T] {
  def get(n: Int): Future[Seq[T]]
}
