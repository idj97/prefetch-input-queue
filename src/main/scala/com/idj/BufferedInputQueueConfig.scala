package com.idj

case class BufferedInputQueueConfig(
    name: String,
    queueSize: Int,
    batchSize: Int,
    maxConcurrency: Int
)
