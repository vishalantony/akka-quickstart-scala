package com.utils

import scala.util.Random

object TestHelpers {
  def getTemperature: Double = (math.floor(Random.nextFloat() * 10000) / 100)
}
