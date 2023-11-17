package com.idj.internal

import castor.{Context, SimpleActor}
import com.idj.Item
import com.idj.internal.Controller._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Promise

class Controller[T](
    conf: ControllerConfig,
    bufService: BufferingService[T],
    initial: Seq[Item[T]] = Seq.empty
)(implicit ctx: Context)
    extends SimpleActor[ControllerMsg]
    with AskableActor[ControllerMsg] {

  private val logger = LoggerFactory.getLogger(s"$getClass:${conf.name}")
  private val queue = mutable.Queue[Item[T]](initial: _*)
  private var buffering = false

  import Protocol._

  def addItems(items: Seq[Item[T]]): Unit = {
    this.send(AddItems(items))
  }

  def bufferingDone(): Unit = {
    this.send(BufferingDone)
  }

  override def run(msg: ControllerMsg): Unit = {
    msg match {
      case Get(n, p) =>
        assert(n > 0)
        val items = mutable.ArrayBuffer[Item[T]]()
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
          val freeSpace = conf.maxQueueSize - queue.length
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
          this.send(StartBuffering)
        }

      case other =>
        logger.warn(s"Unhandled message: $other")
    }
  }

  object Protocol {
    case class Get(n: Int, promise: Promise[Seq[Item[T]]]) extends ControllerMsg
    case object StartBuffering extends ControllerMsg
    case class AddItems(items: Seq[Item[T]]) extends ControllerMsg
    case object BufferingDone extends ControllerMsg

    private[idj] case class Debug(promise: Promise[DebugInfo]) extends ControllerMsg
    private[idj] case class DebugInfo(items: Seq[Item[T]], buffering: Boolean)
  }
}

object Controller {
  trait ControllerMsg
}
