package com.iot

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.concurrent.duration._
import scala.language.postfixOps

// sbt "testOnly com.iot.DeviceTest"

class DeviceTest(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("AkkaQuickstartSpec"))

    override def afterAll: Unit = {
      shutdown(system)
    }

  "reply with empty reading if no temperature is known" in {
    val probe = TestProbe()
    val (groupId, deviceId) = (UUID.randomUUID(), UUID.randomUUID())
    val deviceActor = system.actorOf(Device.props(groupId, deviceId))
    val requestId = 42L
    deviceActor.tell(Device.ReadTemperature(requestId), probe.ref)
    val response = probe.expectMsgType[Device.RespondTemperature]
    response.requestId should ===(requestId)
    response.value should ===(None)
  }

  "reply with temperature reading when temperature is recorded" in {
    val probe = TestProbe()
    val (groupId, deviceId) = (UUID.randomUUID(), UUID.randomUUID())
    val deviceActor = system.actorOf(Device.props(groupId, deviceId))
    val requestId = 42L
    val temperature = 27.8
    deviceActor.tell(Device.RecordTemperature(requestId, temperature), probe.ref)
    val response = probe.expectMsgType[Device.TemperatureRecorded]
    response.requestId should ===(requestId)

    deviceActor.tell(Device.ReadTemperature(requestId + 1), probe.ref)
    val response2 = probe.expectMsgType[Device.RespondTemperature]
    response2.requestId should ===(requestId + 1)
    response2.value should ===(Some(temperature))
  }

  "Reply to registration requests" in {
    val probe = TestProbe()
    val (deviceId, groupId) = (UUID.randomUUID(), UUID.randomUUID())
    val deviceActor = system.actorOf(Device.props(groupId, deviceId))
    deviceActor.tell(DeviceManager.RequestTrackDevice(groupId, deviceId), probe.ref)
    val response = probe.expectMsg(DeviceManager.DeviceRegistered)
    probe.lastSender should ===(deviceActor)
  }

  "Don't reply to registration requests with wrong group or device id" in {
    val probe = TestProbe()
    val (groupId, deviceId) = (UUID.randomUUID(), UUID.randomUUID())
    val device = system.actorOf(Device.props(groupId, deviceId))
    device.tell(DeviceManager.RequestTrackDevice(UUID.randomUUID(), deviceId), probe.ref)
    probe.expectNoMessage(500 milliseconds)

    device.tell(DeviceManager.RequestTrackDevice(groupId, UUID.randomUUID()), probe.ref)
    probe.expectNoMessage(500 milliseconds)

    device.tell(DeviceManager.RequestTrackDevice(UUID.randomUUID(), UUID.randomUUID()), probe.ref)
    probe.expectNoMessage(500 milliseconds)
  }
}
