package com.idj

import castor.Context
import com.idj.internal.{BufferingConfig, BufferingService, Controller, ControllerConfig}

import scala.concurrent.Future

class BufferedInputQueue[T](
    itemSource: ItemSource[T],
    conf: BufferedInputQueueConfig,
    ctx: Context
) {

  private val bufferingConfig = BufferingConfig(conf.batchSize, conf.maxConcurrency)
  private val bufferingService = new BufferingService[T](itemSource, bufferingConfig)(ctx)
  private val controllerConf = ControllerConfig(conf.name, conf.queueSize)
  private val controller = new Controller[T](controllerConf, bufferingService)(ctx)

  def start(): Unit = {
    controller.send(controller.Protocol.StartBuffering)
  }

  def get(n: Int): Seq[Item[T]] = {
    controller.ask[Seq[Item[T]]](p => controller.Protocol.Get(n, p))
  }

  def getAsync(n: Int): Future[Seq[Item[T]]] = {
    controller.askAsync[Seq[Item[T]]](p => controller.Protocol.Get(n, p))
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
