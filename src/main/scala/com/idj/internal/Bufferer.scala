package com.idj.internal

import castor.{Context, SimpleActor}
import com.idj.internal.Bufferer.BuffererMsg
import com.idj.{Item, ItemSource}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.{Failure, Success}

class Bufferer[T](
    conService: ControllerService[T],
    n: Int,
    itemSource: ItemSource[T],
    config: BufferingConfig
)(implicit ctx: Context)
    extends SimpleActor[BuffererMsg] {

  private val logger = LoggerFactory.getLogger(getClass)
  private var buffered = 0
  private var callIdCounter = 1
  private var done = false
  private val ongoingCalls = mutable.Map[Int, Int]()

  this.send(FetchBatch)

  override def run(msg: BuffererMsg): Unit = {
    msg match {
      case FetchBatch =>
        val remaining = n - buffered
        if (remaining == 0 && ongoingCalls.isEmpty && !done) {
          logger.debug(s"Buffering done $buffered/$n, notifying controller")
          conService.bufferingDone()
          done = true
        } else if (remaining > 0 && ongoingCalls.size < config.maxConcurrency) {
          val batchSize = if (remaining >= config.batchSize) config.batchSize else remaining
          val callId = callIdCounter
          callIdCounter += 1
          ongoingCalls.addOne((callId, batchSize))
          buffered += batchSize
          logger.debug(
            s"Fetching batch $callId, " +
              s"size: $batchSize, " +
              s"ongoing calls: ${ongoingCalls.size}/${config.maxConcurrency}"
          )
          itemSource.get(batchSize).onComplete {
            case Success(batch) =>
              this.send(BatchArrived(callId, batch))
            case Failure(ex) =>
              logger.error("Failed to fetch batch", ex)
              this.send(BatchFailed(callId))
          }
          this.send(FetchBatch)
        } else {
          if (remaining > 0) {
            logger.debug(
              s"Waiting (max concurrency reached), " +
                s"ongoing calls: ${ongoingCalls.size}/${config.maxConcurrency}, " +
                s"remaining: $remaining"
            )
          }
        }

      case BatchArrived(callId, items) =>
        logger.debug(
          s"Batch $callId with ${items.length} items arrived, sending items to controller"
        )
        ongoingCalls.remove(callId)
        conService.addItems(items)
        this.send(FetchBatch)

      case BatchFailed(callId) =>
        val batchSize = ongoingCalls(callId)
        logger.debug(s"Fetching batch $callId of $batchSize failed, trying again")
        buffered -= batchSize
        ongoingCalls.remove(callId)
        this.send(FetchBatch)

      case other =>
        logger.warn(s"Unhandled message: $other")
    }
  }

  case object FetchBatch extends BuffererMsg
  case class BatchArrived(callId: Int, items: Seq[Item[T]]) extends BuffererMsg
  case class BatchFailed(callId: Int) extends BuffererMsg
}

object Bufferer {
  trait BuffererMsg
}
