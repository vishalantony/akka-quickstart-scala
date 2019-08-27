package com.iotsystem

import akka.actor.{Actor, ActorLogging, Props}

class IOTSupervisor extends Actor with ActorLogging {
  override def receive: Receive = Actor.emptyBehavior

  override def preStart(): Unit = log.info("IOTSupervisor starting.")

  override def postStop(): Unit = log.info("IOTSupervisor stopping.")
}

object IOTSupervisor {
  def props = Props(new IOTSupervisor)
}

