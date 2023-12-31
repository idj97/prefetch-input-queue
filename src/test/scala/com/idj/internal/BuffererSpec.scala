package com.idj.internal

import castor.Context
import com.idj.ItemSource
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.wordspec.AnyWordSpec

import java.time.Duration
import scala.concurrent.Future

class BuffererSpec extends AnyWordSpec {

  "test" in {
    val maxBatchSize = 3
    val maxConcurrency = 10
    val ctx = new Context.Test()

    val n = 7
    val Seq(i1, i2, i3, i4, i5, i6, i7) = Range(0, 7)
    val itemSource = mock(classOf[ItemSource[Int]])

    doReturn(
      Future.successful(Seq(i1, i2, i3)),
      Future.failed(new Exception("batch failed")),
      Future.successful(Seq(i4, i5, i6))
    ).when(itemSource).get(3)
    doReturn(Future.successful(Seq(i7))).when(itemSource).get(1)

    val controller = mock(classOf[Controller[Int]])
    doNothing().when(controller).addItems(any[Seq[Int]])
    doNothing().when(controller).bufferingDone(7)

    val _ = new Bufferer[Int](controller, itemSource, "test", n, maxBatchSize, maxConcurrency)(ctx)
    Thread.sleep(100)
    ctx.waitForInactivity(Some(Duration.ofHours(1)))

    verify(itemSource, times(3)).get(3)
    verify(itemSource, times(1)).get(1)
    verify(controller, times(1)).addItems(Seq(i1, i2, i3))
    verify(controller, times(1)).addItems(Seq(i4, i5, i6))
    verify(controller, times(1)).addItems(Seq(i7))
    verify(controller, times(1)).bufferingDone(7)
  }
}
