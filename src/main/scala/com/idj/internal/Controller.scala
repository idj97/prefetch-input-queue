package com.idj.internal

import castor.{Context, SimpleActor}
import com.idj.internal.Controller._
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.collection.mutable
import scala.concurrent.Promise

class Controller[T](
    name: String,
    maxQueueSize: Int,
    maxBackoff: Long,
    bufService: BufferingService[T],
    initial: Seq[T] = Seq.empty
)(implicit ctx: Context)
    extends SimpleActor[ControllerMsg]
    with AskableActor[ControllerMsg] {

  private val logger = LoggerFactory.getLogger(s"$getClass:$name")
  private val queue = mutable.Queue[T](initial: _*)
  private var buffering = false
  private var stopped = false
  private var backoffCounter = 0

  import Protocol._

  def addItems(items: Seq[T]): Unit = {
    this.send(AddItems(items))
  }

  def bufferingDone(n: Int): Unit = {
    this.send(BufferingDone(n))
  }

  override def run(msg: ControllerMsg): Unit = {
    msg match {
      case Start =>
        logger.debug("Starting")
        stopped = false
        backoffCounter = 0
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
        p.success(DebugInfo(queue.toSeq, buffering, backoffCounter))

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

      case BufferingDone(n) =>
        if (buffering) {
          logger.debug(s"Buffering done")
          buffering = false

          // if 0 items were buffered then increase backoffCounter
          // otherwise reset it
          if (n == 0) { backoffCounter += 1 }
          else { backoffCounter = 0 }

          if (!stopped) {
            if (backoffCounter == 0) {
              this.send(StartBuffering)
            } else {
              // use backoffCounter to compute backoff time
              val cntBasedBackoff = 100 * Math.pow(2, backoffCounter).toLong
              val backoffMs = if (cntBasedBackoff < maxBackoff) cntBasedBackoff else maxBackoff
              logger.debug(s"Applying buffering backoff of $backoffMs ms")
              ctx.scheduleMsg(this, StartBuffering, Duration.ofMillis(backoffMs))
            }
          }
        }
    }
  }

  object Protocol {
    case class Get(n: Int, promise: Promise[Seq[T]]) extends ControllerMsg
    case object StartBuffering extends ControllerMsg
    case class AddItems(items: Seq[T]) extends ControllerMsg
    case class BufferingDone(n: Int) extends ControllerMsg
    case object Start extends ControllerMsg
    case object Stop extends ControllerMsg
    private[idj] case class Debug(promise: Promise[DebugInfo]) extends ControllerMsg
    private[idj] case class DebugInfo(items: Seq[T], buffering: Boolean, backoffCounter: Int)
  }
}

object Controller {
  trait ControllerMsg
}
