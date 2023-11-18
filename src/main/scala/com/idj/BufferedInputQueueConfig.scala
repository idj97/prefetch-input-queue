package com.idj

case class BufferedInputQueueConfig(
                                     name: String,
                                     maxQueueSize: Int,
                                     batchSize: Int,
                                     maxConcurrency: Int
)
