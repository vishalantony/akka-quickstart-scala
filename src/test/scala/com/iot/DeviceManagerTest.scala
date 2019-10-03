package com.iot

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.util.Random

// sbt "testOnly com.iot.DeviceManagerTest"

class DeviceManagerTest(_system: ActorSystem) extends TestKit(_system)
  with Matchers with WordSpecLike with BeforeAndAfterAll {

  def this() =
    this(ActorSystem("IOTSystem-manager"))

  override def afterAll(): Unit =
    shutdown(system)

  "should create group and device" in {
    val probe = TestProbe()
    val groupId = UUID.randomUUID()
    val deviceId = UUID.randomUUID()

    val manager = system.actorOf(DeviceManager.props)
    manager.tell(DeviceManager.RequestTrackDevice(groupId, deviceId), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val device = probe.lastSender

    val requestId = Random.nextLong()
    val temperature = Random.nextDouble() * 100
    device.tell(Device.RecordTemperature(requestId, temperature), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId))

    device.tell(Device.ReadTemperature(requestId), probe.ref)
    probe.expectMsg(Device.RespondTemperature(requestId, Some(temperature)))
  }

}
