package com.iotsystem

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object DeviceGroup {
  def props(groupId: String) = Props(new DeviceGroup(groupId))
}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {
  var deviceIDToActor = Map.empty[String, ActorRef]
  var actorToDeviceID = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("Device group {} started", groupId)

  override def postStop(): Unit = log.info("Device group {} stopped", groupId)

  override def receive: Receive = {
    case trackMessage@DeviceManager.TrackDeviceRequest(`groupId`, _, _) =>
      deviceIDToActor.get(trackMessage.deviceId) match {
        case Some(deviceActor) =>
          deviceActor.forward(trackMessage)
        case None =>
          log.info("Creating device actor for {}", trackMessage.deviceId)
          val deviceActor = context.actorOf(Device.props(trackMessage.groupId, trackMessage.deviceId),
            s"device-${trackMessage.deviceId}")
          deviceIDToActor += trackMessage.deviceId -> deviceActor
          actorToDeviceID += deviceActor -> trackMessage.deviceId
          deviceActor.forward(trackMessage)
      }

    case DeviceManager.TrackDeviceRequest(g, d, r) =>
      log.info("ignoring request {} {} {}", g, d, r)

    case Terminated(deviceActor) =>
      val deviceId = actorToDeviceID(deviceActor)
      log.info("Device actor for {} has been terminated.", deviceId)
      actorToDeviceID -= deviceActor
      deviceIDToActor -= deviceId
  }
}
