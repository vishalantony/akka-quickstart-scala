package com.example

import akka.actor.{ Actor, ActorSystem, Props }

object SupervisingExperiments2 extends App {
  val system = ActorSystem("supervisory-experiments")
  val a = system.actorOf(SupervisorActor2.props)

  a ! "failChild"

  system.terminate()
}

object ChildActor {
  def props: Props =
    Props(new ChildActor)
}

class ChildActor extends Actor {
  override def preStart(): Unit =
    println("child actor is starting")

  override def postStop(): Unit =
    println("child actor is stopping")

  override def postRestart(reason: Throwable): Unit =
    println("child actor post restart", reason)

  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    println("child actor pre restart", message, reason)

  override def receive: Receive = {
    case "fail" =>
      println("Child is gonna fail!")
      throw new Exception("I quit!")
  }
}

object SupervisorActor2 {
  def props: Props =
    Props(new SupervisorActor2)
}

class SupervisorActor2 extends Actor {
  override def preStart(): Unit =
    println("Supervisor actor is starting")

  override def postStop(): Unit =
    println("supervisor actor is stopping")

  val child = context.actorOf(ChildActor.props, "child")

  override def receive: Receive = {
    case "failChild" => child ! "fail"
  }
}