package com.iotsystem

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}

object Device {
  def props(groupId: String, deviceId: String) =
    Props(new Device(groupId, deviceId))

  final case class ReadTemperature(requestId: UUID)
  final case class RespondTemperature(requestId: UUID, value: Option[Double])

  final case class RecordTemperature(requestId: UUID, value: Double)
  final case class TemperatureRecorded(requestId: UUID)
}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {

  import Device._
  import DeviceManager._

  var lastTemperatureReading: Option[Double] = None

  override def receive: Receive = {
    case ReadTemperature(id) =>
      sender() ! RespondTemperature(id, lastTemperatureReading)

    case RecordTemperature(id, value) =>
      log.info("Recorded temperature reading {} -- {}", id, value)
      lastTemperatureReading = Some(value)
      sender() ! TemperatureRecorded(id)

    case TrackDeviceRequest(`groupId`, `deviceId`, _) =>
      sender() ! DeviceRegistered

    case TrackDeviceRequest(groupId, deviceId, _) =>
      log.info("Ignoring track device request {} {} != {} {}",
        groupId, deviceId, this.groupId, this.deviceId)
  }

  override def preStart(): Unit = log.info("Device actor {} -- {} starting", groupId, deviceId)

  override def postStop(): Unit = log.info("Device actor {} -- {} stopping", groupId, deviceId)
}
