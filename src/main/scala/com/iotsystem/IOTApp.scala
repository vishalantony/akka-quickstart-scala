package com.iotsystem

import akka.actor.ActorSystem

import scala.io.StdIn

object IOTApp extends App {
  val system = ActorSystem("iot-system")

  try {
    val supervisor = system.actorOf(IOTSupervisor.props, "ior-supervisor")
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}
