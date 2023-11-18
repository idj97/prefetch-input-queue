package com.idj.internal

import castor.Context
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, mock, times, verify}
import org.scalatest.wordspec.AnyWordSpec

class ControllerSpec extends AnyWordSpec {
  private val sharedNoopBufService = mock(classOf[BufferingService[Int]])
  private val (i1, i2, i3) = (1, 2, 3)

  private def conf(name: String): ControllerConfig = {
    ControllerConfig(name = name, maxQueueSize = 5)
  }

  "Get" when {

    "empty" in {
      val ctx: Context.Test = new Context.Test()
      val controller: Controller[Int] =
        new Controller[Int](conf("test1"), sharedNoopBufService)(ctx)
      import controller.Protocol.Get
      val items = controller.ask[Seq[Int]](p => Get(n = 1, p))
      assert(items.isEmpty)
    }

    "n < len(queue)" in {
      val ctx: Context.Test = new Context.Test()
      val controller: Controller[Int] =
        new Controller[Int](conf("test2"), sharedNoopBufService, Seq(i1, i2, i3))(ctx)
      import controller.Protocol._

      val requested = controller.ask[Seq[Int]](p => Get(n = 1, p))
      val debug = controller.ask[DebugInfo](p => Debug(p))
      assert(requested == Seq(i1))
      assert(debug.items == Seq(i2, i3))
    }

    "n == len(queue)" in {
      val ctx: Context.Test = new Context.Test()
      val controller: Controller[Int] =
        new Controller[Int](conf("test3"), sharedNoopBufService, Seq(i1, i2, i3))(ctx)
      import controller.Protocol._

      val requested = controller.ask[Seq[Int]](p => Get(n = 3, p))
      val debug = controller.ask[DebugInfo](p => Debug(p))
      assert(requested == Seq(i1, i2, i3))
      assert(debug.items.isEmpty)
    }
  }

  "Buffering" in {

    val ctx: Context.Test = new Context.Test()

    // initialize controller with two buffered items and maxQueueSize of 2
    // and create noopBufService mock
    val bufService = mock(classOf[BufferingService[Int]])
    val config = conf("StartBuffering1")
    val initialItems = Seq(i1, i2)
    val controller: Controller[Int] =
      new Controller[Int](config, bufService, initialItems)(ctx)
    import controller.Protocol._
    val expectedFreeSpace = config.maxQueueSize - initialItems.length
    doNothing()
      .when(bufService)
      .start(any[Controller[Int]], ArgumentMatchers.eq(expectedFreeSpace))

    // start first buffering
    // expected result: controller is in buffering state and bufService is called
    controller.send(StartBuffering)
    ctx.waitForInactivity()
    verify(bufService, times(1))
      .start(any[Controller[Int]], ArgumentMatchers.eq(expectedFreeSpace))
    val debugT1 = controller.ask[DebugInfo](p => Debug(p))
    assert(debugT1.buffering)

    // try to start buffering again (previous one is still in progress)
    // expected result: controller is still in buffering state and bufService is not called
    controller.send(StartBuffering)
    ctx.waitForInactivity()
    verify(bufService, times(1))
      .start(any[Controller[Int]], ArgumentMatchers.eq(expectedFreeSpace))
    val debugT2 = controller.ask[DebugInfo](p => Debug(p))
    assert(debugT2.buffering)

    // add two items
    // expected result: controller is still in buffering state and new buffered items are visible
    val (b1, b2) = (123, 124)
    controller.send(AddItems(Seq(b1, b2)))
    val debugT3 = controller.ask[DebugInfo](p => Debug(p))
    assert(debugT3.buffering)
    assert(debugT3.items == Seq(i1, i2, b1, b2))

    // add one more item
    // expected result: controller is still in buffering state and new buffered items are visible
    val b3 = 125
    controller.send(AddItems(Seq(b3)))
    val debugT4 = controller.ask[DebugInfo](p => Debug(p))
    assert(debugT4.buffering)
    assert(debugT4.items == Seq(i1, i2, b1, b2, b3))

    // complete buffering
    // expected: controller is not in buffering state
    controller.send(BufferingDone)
    ctx.waitForInactivity()
    val debugT5 = controller.ask[DebugInfo](p => Debug(p))
    assert(!debugT5.buffering)

    // complete buffering again (it is already completed)
    // expected: controller is not in buffering state
    controller.send(BufferingDone)
    ctx.waitForInactivity()
    val debugT6 = controller.ask[DebugInfo](p => Debug(p))
    assert(!debugT6.buffering)
  }
}
