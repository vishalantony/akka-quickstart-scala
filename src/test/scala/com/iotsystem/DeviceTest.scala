package com.iotsystem

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._

class DeviceTest(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("DeviceTest"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "reply with empty reading if no temperature is known" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("groupId", "deviceId"))
    val requestId = UUID.randomUUID()
    deviceActor.tell(Device.ReadTemperature(requestId = requestId), probe.ref)
    val response = probe.expectMsgType[Device.RespondTemperature]
    response.requestId should ===(requestId)
    response.value should ===(None)
  }

  "reply with recorded temperature" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("groupId", "deviceId"))
    val requestId = UUID.randomUUID()
    val temperature = 43.4
    deviceActor.tell(Device.RecordTemperature(requestId, temperature), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId))

    deviceActor.tell(Device.ReadTemperature(requestId), probe.ref)
    val response = probe.expectMsgType[Device.RespondTemperature]
    response.requestId should ===(requestId)
    response.value should ===(Some(temperature))
  }

  "reply to registration requests" in {
    val probe = TestProbe()
    val requestId = UUID.randomUUID()
    val group = "group"
    val device = "device"
    val deviceActor = system.actorOf(Device.props(group, device))

    deviceActor.tell(DeviceManager.TrackDeviceRequest(group, device, requestId), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    probe.lastSender should ===(deviceActor)
  }

  "ignore wrong registration requests" in {
    val probe = TestProbe()
    val requestId = UUID.randomUUID()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(DeviceManager.TrackDeviceRequest("wrongGroup", "device", requestId), probe.ref)
    probe.expectNoMessage(500.milliseconds)

    deviceActor.tell(DeviceManager.TrackDeviceRequest("group", "WrongDevice", requestId), probe.ref)
    probe.expectNoMessage(500.milliseconds)
  }
}
