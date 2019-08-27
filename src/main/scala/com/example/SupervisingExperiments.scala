package com.example

import akka.actor.{Actor, ActorSystem, Props}

object SupervisingExperiments extends App {
  val system = ActorSystem("Supervising-experiments")

  val supervisor = system.actorOf(SuperVisingActor.props, "supervising-actor")
  supervisor ! "failChild"

  system.terminate()
}


class SuperVisingActor extends Actor {
  private val child = context.actorOf(SupervisedActor.props, "supervised-actor")

  override def receive: Receive = {
    case "failChild" => child ! "fail"
  }
}


object SuperVisingActor {
  def props: Props = Props(new SuperVisingActor)
}

class SupervisedActor extends Actor {

  override def preStart(): Unit = println("Supervised actor started")

  override def postStop(): Unit = println("Supervised actor stopped")

  override def receive: Receive = {
    case "fail" =>
      println("Supervised actor is failing...")
      throw new Exception("I failed.")
  }
}

object SupervisedActor {
  def props = Props(new SupervisedActor)
}