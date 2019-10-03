package com.iot

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill }
import akka.testkit.{ TestKit, TestProbe }
import com.iot.DeviceGroup.{ ReplyDeviceList, RequestDeviceList }
import com.iot.DeviceManager._
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

// sbt "testOnly com.iot.DeviceGroupTest"

class DeviceGroupTest(_system: ActorSystem) extends TestKit(_system)
  with Matchers with WordSpecLike with BeforeAndAfterAll {

  def this() = this(ActorSystem("IOTSystem"))

  override def afterAll(): Unit =
    shutdown(system)

  "Be able to register a device actor" in {
    val probe      = TestProbe()
    val groupID    = UUID.randomUUID()
    val deviceID1  = UUID.randomUUID()
    val deviceID2  = UUID.randomUUID()
    val groupActor = system.actorOf(DeviceGroup.props(groupID))

    groupActor.tell(RequestTrackDevice(groupID, deviceID1), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val deviceActor1 = probe.lastSender

    groupActor.tell(RequestTrackDevice(groupID, deviceID2), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val deviceActor2 = probe.lastSender

    groupActor.tell(RequestTrackDevice(groupID, deviceID1), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val deviceActor3 = probe.lastSender

    deviceActor1 should !==(deviceActor2)
    deviceActor1 should ===(deviceActor3)

    // check if all devices are working fine
    val requestId   = Random.nextLong()
    val temperature = 78.9

    deviceActor1.tell(Device.RecordTemperature(requestId, temperature), probe.ref)
    probe.expectMsg[Device.TemperatureRecorded](Device.TemperatureRecorded(requestId))
    deviceActor1.tell(Device.ReadTemperature(requestId + 1), probe.ref)
    probe.expectMsg[Device.RespondTemperature](Device.RespondTemperature(requestId + 1, Option(78.9)))

    deviceActor3.tell(Device.ReadTemperature(requestId + 2), probe.ref)
    probe.expectMsg[Device.RespondTemperature](Device.RespondTemperature(requestId + 2, Option(78.9)))

    deviceActor2.tell(Device.ReadTemperature(requestId + 3), probe.ref)
    probe.expectMsg[Device.RespondTemperature](Device.RespondTemperature(requestId + 3, None))
  }

  "Device group should ignore message with wrong groupID" in {
    val probe      = TestProbe()
    val groupId    = UUID.randomUUID()
    val groupActor = system.actorOf(DeviceGroup.props(groupId))
    groupActor.tell(RequestTrackDevice(UUID.randomUUID(), UUID.randomUUID()), probe.ref)
    probe.expectNoMessage(500 milliseconds)
  }

  "be able to list active devices" in {
    val probe     = TestProbe()
    val deviceId1 = UUID.randomUUID()
    val deviceId2 = UUID.randomUUID()
    val groupId   = UUID.randomUUID()
    val requestId = Random.nextLong()

    val deviceGroup = system.actorOf(DeviceGroup.props(groupId))
    deviceGroup.tell(RequestTrackDevice(groupId, deviceId1), probe.ref)
    probe.expectMsg(DeviceRegistered)

    deviceGroup.tell(RequestTrackDevice(groupId, deviceId2), probe.ref)
    probe.expectMsg(DeviceRegistered)

    deviceGroup.tell(RequestDeviceList(requestId), probe.ref)
    probe.expectMsg(ReplyDeviceList(requestId, Set(deviceId1, deviceId2)))
  }

  "be able to list active devices after one terminates" in {
    val probe     = TestProbe()
    val deviceId1 = UUID.randomUUID()
    val deviceId2 = UUID.randomUUID()
    val groupId   = UUID.randomUUID()
    val requestId = Random.nextLong()

    val deviceGroup = system.actorOf(DeviceGroup.props(groupId))
    deviceGroup.tell(RequestTrackDevice(groupId, deviceId1), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val deviceActor = probe.lastSender

    deviceGroup.tell(RequestTrackDevice(groupId, deviceId2), probe.ref)
    probe.expectMsg(DeviceRegistered)

    deviceGroup.tell(RequestDeviceList(requestId), probe.ref)
    probe.expectMsg(ReplyDeviceList(requestId, Set(deviceId1, deviceId2)))

    probe.watch(deviceActor)
    deviceActor ! PoisonPill
    probe.expectTerminated(deviceActor)

    probe.awaitAssert {
      deviceGroup.tell(RequestDeviceList(requestId), probe.ref)
      probe.expectMsg(ReplyDeviceList(requestId, Set(deviceId2)))
    }
  }
}
