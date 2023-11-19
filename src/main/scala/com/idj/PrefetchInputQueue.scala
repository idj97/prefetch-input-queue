package com.idj

import castor.Context
import com.idj.internal.{BufferingService, Controller}

import scala.concurrent.Future

class PrefetchInputQueue[T](
    itemSource: ItemSource[T],
    conf: PrefetchInputQueueConfig,
    ctx: Context
) {

  private val bufferingService =
    new BufferingService[T](itemSource, conf.name, conf.maxBatchSize, conf.maxConcurrency)(ctx)
  private val controller =
    new Controller[T](conf.name, conf.maxQueueSize, conf.maxBackoff.toMillis, bufferingService)(ctx)

  def start(): Unit = {
    controller.send(controller.Protocol.Start)
  }

  def get(n: Int): Seq[T] = {
    controller.ask[Seq[T]](p => controller.Protocol.Get(n, p))
  }

  def getAsync(n: Int): Future[Seq[T]] = {
    controller.askAsync[Seq[T]](p => controller.Protocol.Get(n, p))
  }

  def stop(): Unit = {
    controller.send(controller.Protocol.Stop)
  }
}

object PrefetchInputQueue {

  def create[T](
      itemSource: ItemSource[T],
      conf: PrefetchInputQueueConfig
  ): PrefetchInputQueue[T] = {
    new PrefetchInputQueue[T](itemSource, conf, Context.Simple.global)
  }

  def create[T](
      itemSource: ItemSource[T],
      conf: PrefetchInputQueueConfig,
      ctx: Context
  ): PrefetchInputQueue[T] = {
    new PrefetchInputQueue[T](itemSource, conf, ctx)
  }

}
