package com.iotsystem

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import com.utils.TestHelpers._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class DeviceGroupTest(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("DeviceTest"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  def testDevice(deviceActor1: ActorRef, probe: TestProbe): Unit = {
    val requestId = UUID.randomUUID()
    val temperature = getTemperature
    deviceActor1.tell(Device.RecordTemperature(requestId, temperature), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId))

    val requestId2 = UUID.randomUUID()
    deviceActor1.tell(Device.ReadTemperature(requestId2), probe.ref)
    probe.expectMsg(Device.RespondTemperature(requestId2, Some(temperature)))
  }

  "be able to register a device" in {
    val probe = TestProbe()
    val groupID = "group"

    val groupActor = system.actorOf(DeviceGroup.props(groupID))

    groupActor.tell(DeviceManager.TrackDeviceRequest(groupID, "device1", UUID.randomUUID()), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    groupActor.tell(DeviceManager.TrackDeviceRequest(groupID, "device2", UUID.randomUUID()), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender
    deviceActor1 should !==(deviceActor2)

    testDevice(deviceActor1, probe)
    testDevice(deviceActor2, probe)
  }

  "ignore requests for wrong groupId" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("group"))
    groupActor.tell(DeviceManager.TrackDeviceRequest("wrongGroup", "device1", UUID.randomUUID()), probe.ref)
    probe.expectNoMessage(500.milliseconds)
  }

  "return same device for existing device id" in {
    val probe = TestProbe()
    val groupID = "group"
    val deviceId = "device"
    val groupActor = system.actorOf(DeviceGroup.props(groupID))

    groupActor.tell(DeviceManager.TrackDeviceRequest(groupID, deviceId, UUID.randomUUID()), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    groupActor.tell(DeviceManager.TrackDeviceRequest(groupID, deviceId, UUID.randomUUID()), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender
    deviceActor1 should ===(deviceActor2)
  }

}
