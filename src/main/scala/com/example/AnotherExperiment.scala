package com.example

import akka.actor.{Actor, ActorSystem, Props}

import scala.io.StdIn

class PrintMyActorRefActor1 extends Actor {
  override def receive: Receive = {
    case "printit" =>
      val secondActorRef = context.actorOf(Props.empty, "second-actor")
      println(s"second actor $secondActorRef")
  }
}

object PrintMyActorRefActor1 {
  def props: Props =
    Props(new PrintMyActorRefActor1)
}

object AnotherExperiment extends App {
  val system = ActorSystem("testSystem")

  val actor1 = system.actorOf(PrintMyActorRefActor1.props, "first-actor")
  println(s"actor1 ref: $actor1")
  actor1 ! "printit"

  println("press enter to exit")
  try StdIn.readLine()
  finally system.terminate()
}
