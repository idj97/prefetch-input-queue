package com.idj.internal

import castor.{Context, SimpleActor}
import com.idj.ItemSource
import com.idj.internal.Bufferer.BuffererMsg
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.{Failure, Success}

class Bufferer[T](
    controller: Controller[T],
    itemSource: ItemSource[T],
    name: String,
    n: Int,
    maxBatchSize: Int,
    maxConcurrency: Int
)(implicit ctx: Context)
    extends SimpleActor[BuffererMsg] {

  private val logger = LoggerFactory.getLogger(s"$getClass:$name")
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
          controller.bufferingDone()
          done = true
        } else if (remaining > 0 && ongoingCalls.size < maxConcurrency) {
          val batchSize = if (remaining >= maxBatchSize) maxBatchSize else remaining
          val callId = callIdCounter
          callIdCounter += 1
          ongoingCalls.addOne((callId, batchSize))
          buffered += batchSize
          logger.debug(
            s"Fetching batch $callId, " +
              s"size: $batchSize, " +
              s"ongoing calls: ${ongoingCalls.size}/$maxConcurrency"
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
                s"ongoing calls: ${ongoingCalls.size}/$maxConcurrency, " +
                s"remaining: $remaining"
            )
          }
        }

      case BatchArrived(callId, items) =>
        logger.debug(
          s"Batch $callId with ${items.length} items arrived, sending items to controller"
        )
        ongoingCalls.remove(callId)
        if (items.nonEmpty) {
          controller.addItems(items)
        } else {
          done = true
        }
        this.send(FetchBatch)

      case BatchFailed(callId) =>
        val batchSize = ongoingCalls(callId)
        logger.debug(s"Fetching batch $callId of $batchSize failed, trying again")
        buffered -= batchSize
        ongoingCalls.remove(callId)
        this.send(FetchBatch)
    }
  }

  case object FetchBatch extends BuffererMsg
  case class BatchArrived(callId: Int, items: Seq[T]) extends BuffererMsg
  case class BatchFailed(callId: Int) extends BuffererMsg
}

object Bufferer {
  trait BuffererMsg
}
