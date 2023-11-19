package com.idj

import scala.concurrent.duration.{Duration, DurationInt}

case class BufferedInputQueueConfig(
    name: String,
    maxQueueSize: Int,
    maxBatchSize: Int,
    maxConcurrency: Int,
    maxBackoff: Duration = 1.second
)
