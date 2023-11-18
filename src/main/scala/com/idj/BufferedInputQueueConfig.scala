package com.idj

case class BufferedInputQueueConfig(
    name: String,
    maxQueueSize: Int,
    maxBatchSize: Int,
    maxConcurrency: Int
)
