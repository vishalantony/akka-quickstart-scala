package com.example

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}

import scala.io.StdIn

object PrintMyActorRefActor {
  def props: Props = Props(new PrintMyActorRefActor)
}

class PrintMyActorRefActor extends Actor {
  override def receive: Receive = {
    case "printit" =>
      val secondRef = context.actorOf(Props.empty, "second-actor")
      println(s" SecondRef: $secondRef")
  }
}

object ActorHeirarchyExperiments extends App {
 val system = ActorSystem("testSystem")

  val firstRef = system.actorOf(PrintMyActorRefActor.props, "first-actor")
  println(s"firstactor: $firstRef")
  firstRef ! "printit"

//  firstRef ! PoisonPill

  val first = system.actorOf(StartStopActor1.prop, "first-ss")
  first ! "stop"

  println(">>> Press ENTER to exit")
  try StdIn.readLine()
  finally system.terminate()
}


class StartStopActor1 extends Actor {
  override def receive: Receive = {
    case "stop" => context.stop(self)
  }

  override def preStart(): Unit = {
    println("first started")
    context.actorOf(StartStopActor2.prop, "second")
  }

  override def postStop(): Unit = {
    println("first stopped")
  }
}

object StartStopActor1 {
  def prop: Props = Props(new StartStopActor1)
}

class StartStopActor2 extends Actor {
  override def receive: Receive = Actor.emptyBehavior

  override def postStop(): Unit = println("second stopped")

  override def preStart(): Unit = println("second started")
}

object StartStopActor2 {
  def prop = Props(new StartStopActor2)
}