package com.example

import akka.actor.{ Actor, ActorSystem, PoisonPill, Props }

object StartStopExperiment extends App {
  val system = ActorSystem("StartStopExperiment")

  val a = system.actorOf(StartStopActor1Another.props, "first")

//  a ! "stop"

  a ! PoisonPill
  system.terminate()
}

object StartStopActor1Another {
  def props: Props = Props(new StartStopActor1Another)
}

class StartStopActor1Another extends Actor {
  override def preStart(): Unit = {
    println("first started")
    context.actorOf(StartStopActor2Another.props, "second")
  }

  override def postStop(): Unit = println("first stopped")

//  override def receive: Receive = {
//    case "stop" => context.stop(self)
//  }

  override def receive: Receive = Actor.emptyBehavior
}


object StartStopActor2Another {
  def props : Props = Props(new StartStopActor2Another)
}

class StartStopActor2Another extends Actor {
  override def preStart(): Unit =
    println("second started")

  override def postStop(): Unit =
    println("second stopped")

  override def receive: Receive = Actor.emptyBehavior
}