# Prefetch Input Queue

Prefetch Input Queue is a lightweight, thread-safe library built with Scala and actors for educational purposes. 
It is designed to improve the efficiency of multithreaded programs that follow producer-consumer pattern by 
prefetching/buffering the data from source systems and providing it to consumers, all in a thread-safe manner.

- [x] Continuous prefetching / buffering
- [x] Exponential backoff mechanism
- [x] Configurability
- [ ] Published to central Maven Repo

## Install

Add library to `build.sbt`:

```scala
"com.idj" % "prefetch-input-queue" %% $VERSION
```
Artifacts are published to central Maven Repository, link coming soon.

## Usage

First implement custom `ItemSource`:

```scala
import com.idj.ItemSource
import scala.util.Random

class MyRandomIntsSource() extends ItemSource[Int] {

  override def get(n: Int): Future[Seq[Int]] = {
    Future.successful {
      Seq.fill(n)(Random.nextInt())
    }
  }
}
```

Then configure, create and start PrefetchInputQueue:

```scala 
import com.idj.{PrefetchInputQueueConfig, PrefetchInputQueue}

val source = new MyRandomIntsSource()
val config = PrefetchInputQueueConfig(
  name = "myInputQueue",
  maxQueueSize = 1000,
  maxConcurrency = 10,
  maxBatchSize = 20,
  maxBackoff = 5.seconds
)
val queue = PrefetchInputQueue.create(source, config)
queue.start()
```

Consume items from the queue, in sync or async fashion. In case when queue is empty result value is returned
immediately and is also empty:

```scala
val items: Seq[Int] = queue.get(n = 100) // sync api
val itemsFuture: Future[Int] = queue.getAsync(n = 100) // async api
```

Stop prefetching/buffering:
```scala
queue.stop() // can be resumed by calling start() again
```

## Configuration

| Name           | Description                                                  | Default  |
|----------------|--------------------------------------------------------------|----------|
| name           | Name used for logging                                        |          |
| maxQueueSize   | Maximum number of items to prefetch/buffer                   |          |
| maxConcurrency | Maximum number of concurrent fetch calls made to item source |          |
| maxBatchSize   | Maximum number of items to fetch in each call                |          |
| maxBackoff     | Maximum wait time used in backoff mechanism                  | 1 second |

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
