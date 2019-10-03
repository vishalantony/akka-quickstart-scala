package com.iot

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, Props }

object Device {
  def props(groupId: UUID, deviceId: UUID): Props =
    Props(new Device(groupId, deviceId))

  final case class ReadTemperature(requestId: Long)
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  final case class RecordTemperature(requestId: Long, temperature: Double)
  final case class TemperatureRecorded(requestId: Long)
}

class Device(groupId: UUID, deviceId: UUID) extends Actor with ActorLogging {

  import Device._

  var lastTemperatureReading: Option[Double] = None

  override def receive: Receive = {
    case ReadTemperature(rqId) =>
      sender() ! RespondTemperature(rqId, lastTemperatureReading)

    case RecordTemperature(rqId, temp) =>
      lastTemperatureReading = Some(temp)
      sender() ! TemperatureRecorded(rqId)

    case DeviceManager.RequestTrackDevice(`groupId`, `deviceId`) =>
      sender() ! DeviceManager.DeviceRegistered

    case DeviceManager.RequestTrackDevice(gid, did) =>
      log.warning("Ignoring track request expected: {}-{} actual: {}-{}",
        groupId, deviceId, gid, did)
  }

  override def preStart(): Unit =
    log.info("Device {} - {} started", groupId, deviceId)

  override def postStop(): Unit =
    log.info("Device {} - {} stopped", groupId, deviceId)
}
