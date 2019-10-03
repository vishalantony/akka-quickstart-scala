package com.iot

import akka.actor.{ Actor, ActorLogging, Props }

class IotSupervisor extends Actor with ActorLogging {
  override def preStart(): Unit =
    log.info("starting iot supervisor")

  override def postStop(): Unit =
    log.info("stopping iot supervisor")

  override def receive: Receive = Actor.emptyBehavior
}

object IotSupervisor {
  def props : Props =
    Props(new IotSupervisor)
}