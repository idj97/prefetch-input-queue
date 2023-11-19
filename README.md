# Buffered Input Queue

Buffered Input Queue is a lightweight, thread-safe Scala library created with Castor actors library for learning purposes. 
It is designed to enhance the efficiency of multithreaded programs that prefetching/buffering the data from remote systems
and serving it to other software components, all in a thread-safe way.

- [x] Continuous prefetching / buffering
- [x] Exponential backoff mechanism
- [x] Configurability
- [ ] Published to central Maven Repo

## Install

Add library to `build.sbt`:

```scala
"com.idj" % "buffered-input-queue" %% $VERSION
```
Artifacts are published to central Maven Repository, link coming soon.

## Usage

First implement custom `ItemSource`:

```scala
import com.idj.ItemSource

class MyItemSource() extends ItemSource[Int] {

  override def get(n: Int): Future[Seq[Int]] = {
    ???
  }
}
```

Then configure, create and start BufferedInputQueue:

```scala 

val itemSource = new MyItemSource()
val config = BufferedInputQueueConfig(
  name = "myInputQueue",
  maxQueueSize = 1000,
  maxConcurrency = 10,
  maxBatchSize = 20,
  maxBackoff = 5.seconds
)
val bufInQueue = BufferedInputQueue.create(itemSource, config)
bufInQueue.start()
```

Consume items from the queue, in sync or async fashion. In case when queue is empty result value is returned
immediately and is also empty:

```scala
val items: Seq[Int] = bufInQueue.get(n = 100) // sync api
val itemsFuture: Future[Int] = bufInQueue.getAsync(n = 100) // async api
```

Stop prefetching/buffering:
```scala
bufInQueue.stop() // can be resumed by calling start() again
```

## Configuration

| Name           | Description                                                  |
|----------------|--------------------------------------------------------------|
| name           | Name of the queue used for logging                           |
| maxQueueSize   | Maximum number of items to prefetch/buffer                   |
| maxConcurrency | Maximum number of concurrent fetch calls made to item source |
| maxBatchSize   | Maximum number of items to fetch in each call                |
| maxBackoff     | Maximum wait time used in backoff mechanism                  |

## License

MIT License

Copyright (c) 2023

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
