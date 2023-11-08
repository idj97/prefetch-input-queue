package com.idj.internal

import castor.Context
import com.idj.{Item, ItemSource}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class BuffererSpec extends AnyWordSpec {

  "test" in {
    val config = BufferingConfig(batchSize = 3, maxConcurrency = 10)
    val ctx = new Context.Test()

    val n = 7
    val Seq(i1, i2, i3, i4, i5, i6, i7) = Range(0, 7).map(i => Item(i, Instant.now()))
    val itemSource = mock(classOf[ItemSource[Int]])

    doReturn(
      Future { Thread.sleep(500); Seq(i1, i2, i3) }(ExecutionContext.global),
      Future.successful(Seq(i4, i5, i6))
    ).when(itemSource).get(3)
    doReturn(Future.successful(Seq(i7))).when(itemSource).get(1)

    val conService = mock(classOf[ControllerService[Int]])
    doNothing().when(conService).addItems(any[Seq[Item[Int]]])
    doNothing().when(conService).bufferingDone()

    val _ = new Bufferer[Int](conService, n, itemSource, config)(ctx)
    Thread.sleep(1000)
    ctx.waitForInactivity(Some(Duration.ofHours(1)))

    verify(itemSource, times(2)).get(3)
    verify(itemSource, times(1)).get(1)
    verify(conService, times(1)).addItems(Seq(i1, i2, i3))
    verify(conService, times(1)).addItems(Seq(i4, i5, i6))
    verify(conService, times(1)).addItems(Seq(i7))
    verify(conService, times(1)).bufferingDone()
  }
}
