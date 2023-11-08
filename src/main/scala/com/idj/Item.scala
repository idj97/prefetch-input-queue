package com.idj

import java.time.Instant

case class Item[T] (value: T, createdAt: Instant)
