package com.idj.internal

import castor.Context
import com.idj.ItemSource

class BufferingService[T](
    itemSource: ItemSource[T],
    config: BufferingConfig
)(implicit ctx: Context) {

  def start(controller: Controller[T], n: Int): Unit = {
    new Bufferer[T](controller, n, itemSource, config)
  }
}
