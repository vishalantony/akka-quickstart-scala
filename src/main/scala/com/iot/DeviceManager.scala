package com.iot

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }
import com.iot.DeviceManager.RequestTrackDevice

object DeviceManager {

  def props: Props =
    Props(new DeviceManager)

  case class RequestTrackDevice(groupId: UUID, deviceId: UUID)
  case object DeviceRegistered

}

class DeviceManager extends Actor with ActorLogging {
  var groupIdToActor = Map.empty[UUID, ActorRef]
  var actorToGroupID = Map.empty[ActorRef, UUID]

  override def postStop(): Unit = log.info("stopped device manager")

  override def preStart(): Unit = log.info("started device manager")

  override def receive: Receive = {
    case trackReq@RequestTrackDevice(groupId, _) =>
      groupIdToActor.get(groupId) match {
        case None =>
          log.info("creating new group actor {}", groupId)
          val groupActor = context.actorOf(DeviceGroup.props(groupId))
          context.watch(groupActor)
          groupActor.forward(trackReq)
          groupIdToActor += groupId -> groupActor
          actorToGroupID += groupActor -> groupId

        case Some(groupActor) =>
          groupActor.forward(trackReq)
      }


    case Terminated(groupActor) =>
      val groupId = actorToGroupID(groupActor)
      log.info("Device group actor for {} terminated", groupId)
      actorToGroupID -= groupActor
      groupIdToActor -= groupId
  }
}