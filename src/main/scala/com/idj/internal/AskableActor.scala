package com.idj.internal

import castor.SimpleActor

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

trait AskableActor[In] { a: SimpleActor[In] =>

  def ask[Out](f: Promise[Out] => In)(implicit timeout: Duration = Duration.Inf): Out = {
    Await.result(askAsync(f), timeout)
  }

  def askAsync[Out](f: Promise[Out] => In): Future[Out] = {
    val promise: Promise[Out] = Promise[Out]()
    a.send(f(promise))
    promise.future
  }

}
