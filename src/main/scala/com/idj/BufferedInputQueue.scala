package com.idj

import castor.Context
import com.idj.internal.{BufferingService, Controller}

import scala.concurrent.Future

class BufferedInputQueue[T](
    itemSource: ItemSource[T],
    conf: BufferedInputQueueConfig,
    ctx: Context
) {

  private val bufferingService =
    new BufferingService[T](itemSource, conf.name, conf.maxBatchSize, conf.maxConcurrency)(ctx)
  private val controller =
    new Controller[T](conf.name, conf.maxQueueSize, bufferingService)(ctx)

  def start(): Unit = {
    controller.send(controller.Protocol.StartBuffering)
  }

  def get(n: Int): Seq[T] = {
    controller.ask[Seq[T]](p => controller.Protocol.Get(n, p))
  }

  def getAsync(n: Int): Future[Seq[T]] = {
    controller.askAsync[Seq[T]](p => controller.Protocol.Get(n, p))
  }
}

object BufferedInputQueue {

  def create[T](
      itemSource: ItemSource[T],
      conf: BufferedInputQueueConfig
  ): BufferedInputQueue[T] = {
    new BufferedInputQueue[T](itemSource, conf, Context.Simple.global)
  }

  def create[T](
      itemSource: ItemSource[T],
      conf: BufferedInputQueueConfig,
      ctx: Context
  ): BufferedInputQueue[T] = {
    new BufferedInputQueue[T](itemSource, conf, ctx)
  }

}
