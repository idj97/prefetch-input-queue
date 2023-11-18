package com.idj

import castor.Context
import org.scalatest.wordspec.AnyWordSpec

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class BufferedInputQueueSpec extends AnyWordSpec {

  // Functional/integration test which verifies
  // that behavior of whole system is correct
  "test" in {
    val itemSource = new FakeUnorderedItemSource(120)
    val conf = BufferedInputQueueConfig(
      name = "itTest1",
      maxQueueSize = 50,
      maxBatchSize = 10,
      maxConcurrency = 2
    )
    val bufInQueue = BufferedInputQueue.create(itemSource, conf, Context.Simple.global)
    bufInQueue.start()
    val itemsAccumulator = mutable.Set[Int]()

    // Wait 100ms and try to get 60 items from queue
    // because maxQueueSize is 50 we will get 50
    // check if items were seen before to make sure that
    // buffered queue is not delivering duplicates
    Thread.sleep(100)
    val items1 = bufInQueue.get(60)
    assert(items1.length == 50)
    items1.foreach(i => assert(!itemsAccumulator.contains(i)))
    itemsAccumulator.addAll(items1)

    // repeat for the next 50 items
    Thread.sleep(100)
    // make one of the get calls using async API
    val items2 = Await.result(bufInQueue.getAsync(60), Duration.Inf)
    assert(items2.length == 50)
    items2.foreach(i => assert(!itemsAccumulator.contains(i)))
    itemsAccumulator.addAll(items2)

    // repeat for the last 20 items
    Thread.sleep(100)
    val items3 = bufInQueue.get(60)
    assert(items3.length == 20)
    items3.foreach(i => assert(!itemsAccumulator.contains(i)))
    itemsAccumulator.addAll(items3)

    // at this point queue is empty
    Thread.sleep(100)
    val items4 = bufInQueue.get(600)
    assert(items4.isEmpty)

    // shutdown queue
    bufInQueue.stop()
  }
}

class FakeUnorderedItemSource(count: Int) extends ItemSource[Int] {

  private val queue = mutable.Queue[Int]()
  queue.enqueueAll(Range.inclusive(1, count))

  override def get(n: Int): Future[Seq[Int]] = {
    Future {
      queue.synchronized {
        get(n, Seq.empty)
      }
    }(ExecutionContext.global)
  }

  @tailrec
  private def get(n: Int, acc: Seq[Int]): Seq[Int] = {
    if (n == 0 || queue.isEmpty) {
      acc
    } else {
      val item = queue.dequeue()
      get(n - 1, acc :+ item)
    }
  }
}
