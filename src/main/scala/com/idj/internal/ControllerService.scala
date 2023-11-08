package com.idj.internal

import com.idj.Item

class ControllerService[T](controller: Controller[T]) {

  import controller.Protocol.{AddItems, BufferingDone}

  def addItems(items: Seq[Item[T]]): Unit = {
    controller.send(AddItems(items))
  }

  def bufferingDone(): Unit = {
    controller.send(BufferingDone)
  }

}
