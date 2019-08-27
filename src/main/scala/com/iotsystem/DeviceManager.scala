package com.iotsystem

import java.util.UUID

import akka.actor.{Actor, Props}

object DeviceManager {
  def props: Props = ???

  case class TrackDeviceRequest(groupId: String, deviceId: String, requestID: UUID)
  case object DeviceRegistered
}

class DeviceManager extends Actor {
  override def receive: Receive = ???
}
