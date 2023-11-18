package com.idj.internal

import castor.Context
import com.idj.ItemSource

class BufferingService[T](
    itemSource: ItemSource[T],
    name: String,
    maxBatchSize: Int,
    maxConcurrency: Int
)(implicit ctx: Context) {

  def start(controller: Controller[T], n: Int): Unit = {
    new Bufferer[T](controller, itemSource, name, n, maxBatchSize, maxConcurrency)
  }
}
