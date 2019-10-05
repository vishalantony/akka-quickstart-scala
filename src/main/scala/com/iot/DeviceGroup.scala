package com.iot

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }
import com.iot.DeviceGroup.{ ReplyDeviceList, RequestAllTemperatures, RequestDeviceList }
import com.iot.DeviceManager.RequestTrackDevice

import scala.concurrent.duration._

class DeviceGroup(groupId: UUID) extends Actor with ActorLogging {
  var deviceIdToDeviceActor = Map.empty[UUID, ActorRef]
  var deviceActorToDeviceId = Map.empty[ActorRef, UUID]

  override def preStart(): Unit =
    log.info("Starting device group {} ", groupId)

  override def postStop(): Unit =
    log.info("Stopping device group {} ", groupId)

  override def receive: Receive = {
    case trackMessage@RequestTrackDevice(`groupId`, deviceId) =>
      deviceIdToDeviceActor.get(deviceId) match {
        case None =>
          log.info("Creating device actor for {}", deviceId)
          val deviceActor = context.actorOf(Device.props(groupId, deviceId),
            s"device-gid$groupId-did$deviceId")
          context.watch(deviceActor)
          deviceIdToDeviceActor += (deviceId -> deviceActor)
          deviceActorToDeviceId += (deviceActor -> deviceId)
          deviceActor forward trackMessage

        case Some(deviceActor) => deviceActor forward trackMessage
      }
    case RequestTrackDevice(groupId, deviceId) =>
      log.warning("ignore track device for {} - {}", groupId, deviceId)

    case Terminated(deviceActor) =>
      val deviceId = deviceActorToDeviceId(deviceActor)
      log.info("Device actor for {} is terminated", deviceId)
      deviceIdToDeviceActor -= deviceId
      deviceActorToDeviceId -= deviceActor

    case RequestDeviceList(requestId) =>
      sender() ! ReplyDeviceList(requestId, deviceIdToDeviceActor.keySet)

    case RequestAllTemperatures(requestId) =>
      context.actorOf(DeviceGroupQuery.props(deviceActorToDeviceId, requestId, sender(), 3 seconds))
  }
}

object DeviceGroup {
  def props(groupId: UUID): Props =
    Props(new DeviceGroup(groupId))

  final case class RequestDeviceList(requestId: Long)

  final case class ReplyDeviceList(requestId: Long, deviceList: Set[UUID])

  sealed trait TemperatureReading

  final case class Temperature(value: Double) extends TemperatureReading

  case object TemperatureNotAvailable extends TemperatureReading

  case object DeviceNotAvailable extends TemperatureReading

  case object DeviceTimedOut extends TemperatureReading

  final case class RequestAllTemperatures(requestId: Long)

  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[UUID, TemperatureReading])

}