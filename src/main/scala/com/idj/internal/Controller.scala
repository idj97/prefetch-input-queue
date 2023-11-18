package com.idj.internal

import castor.{Context, SimpleActor}
import com.idj.internal.Controller._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Promise

class Controller[T](
    name: String,
    maxQueueSize: Int,
    bufService: BufferingService[T],
    initial: Seq[T] = Seq.empty
)(implicit ctx: Context)
    extends SimpleActor[ControllerMsg]
    with AskableActor[ControllerMsg] {

  private val logger = LoggerFactory.getLogger(s"$getClass:$name")
  private val queue = mutable.Queue[T](initial: _*)
  private var buffering = false
  private var stopped = false

  import Protocol._

  def addItems(items: Seq[T]): Unit = {
    this.send(AddItems(items))
  }

  def bufferingDone(): Unit = {
    this.send(BufferingDone)
  }

  override def run(msg: ControllerMsg): Unit = {
    msg match {
      case Start =>
        logger.debug("Starting")
        stopped = false
        this.send(StartBuffering)

      case Stop =>
        logger.debug("Stopping")
        stopped = true

      case Get(n, p) =>
        assert(n > 0)
        val items = mutable.ArrayBuffer[T]()
        for (_ <- 1 to n) {
          if (queue.nonEmpty) {
            items.addOne(queue.dequeue())
          }
        }
        p.success(items.toSeq)
        if (!buffering) {
          this.send(StartBuffering)
        }

      case Debug(p) =>
        p.success(DebugInfo(queue.toSeq, buffering))

      case StartBuffering =>
        if (!buffering) {
          val freeSpace = maxQueueSize - queue.length
          if (freeSpace > 0) {
            logger.debug(s"Starting buffering, free space: $freeSpace")
            val _ = bufService.start(this, freeSpace)
            buffering = true
          }
        }

      case AddItems(items) =>
        logger.debug(s"Adding ${items.length} items")
        queue.enqueueAll(items)

      case BufferingDone =>
        if (buffering) {
          logger.debug(s"Buffering done")
          buffering = false
          if (!stopped) {
            this.send(StartBuffering)
          }
        }
    }
  }

  object Protocol {
    case class Get(n: Int, promise: Promise[Seq[T]]) extends ControllerMsg
    case object StartBuffering extends ControllerMsg
    case class AddItems(items: Seq[T]) extends ControllerMsg
    case object BufferingDone extends ControllerMsg
    case object Start extends ControllerMsg
    case object Stop extends ControllerMsg
    private[idj] case class Debug(promise: Promise[DebugInfo]) extends ControllerMsg
    private[idj] case class DebugInfo(items: Seq[T], buffering: Boolean)
  }
}

object Controller {
  trait ControllerMsg
}
