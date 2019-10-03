package com.iot

import akka.actor.ActorSystem

import scala.io.StdIn

object IotMain extends App {
  val system = ActorSystem ("iot-system")
  try {
    val supervisor = system.actorOf(IotSupervisor.props, "supervisor")
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}
