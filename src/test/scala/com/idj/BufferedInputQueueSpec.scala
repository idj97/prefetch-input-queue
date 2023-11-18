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
    val conf = BufferedInputQueueConfig("test1", 50, 10, 2)
    val ctx = new Context.Test()
    val bufInQueue = BufferedInputQueue.create(itemSource, conf, ctx)
    bufInQueue.start()

    val itemsAccumulator = mutable.Set[Int]()
    Thread.sleep(100)
    val items1 = bufInQueue.get(60)
    assert(items1.length == 50)
    items1.foreach(i => assert(!itemsAccumulator.contains(i)))
    itemsAccumulator.addAll(items1)

    Thread.sleep(100)
    // make one of the get calls using async API
    val items2 = Await.result(bufInQueue.getAsync(60), Duration.Inf)
    assert(items2.length == 50)
    items2.foreach(i => assert(!itemsAccumulator.contains(i)))
    itemsAccumulator.addAll(items2)

    Thread.sleep(100)
    val items3 = bufInQueue.get(600)
    assert(items3.length == 20)
    items3.foreach(i => assert(!itemsAccumulator.contains(i)))
    itemsAccumulator.addAll(items3)

    Thread.sleep(100)
    val items4 = bufInQueue.get(600)
    assert(items4.isEmpty)
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