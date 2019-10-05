package com.iot

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill }
import akka.testkit.{ TestKit, TestProbe }
import com.iot.DeviceGroup.{ DeviceNotAvailable, DeviceTimedOut, Temperature, TemperatureNotAvailable }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.concurrent.duration._
import scala.language.postfixOps

// sbt "testOnly com.iot.DeviceGroupQueryTest"
class DeviceGroupQueryTest(_system: ActorSystem) extends TestKit(_system)
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("IOTSystem-Query"))

  override def afterAll(): Unit =
    shutdown(system)

  "return temperature from working devices" in {
    val requesterActor = TestProbe()
    val device1        = TestProbe()
    val device2        = TestProbe()
    val device1Id      = UUID.randomUUID()
    val device2Id      = UUID.randomUUID()
    val mapper         = Map(device1.ref -> device1Id, device2.ref -> device2Id)
    val reqId          = 0

    val queryActor = system.actorOf(DeviceGroupQuery.props(mapper, reqId, requesterActor.ref, 3 seconds))
    device1.expectMsg(Device.ReadTemperature(reqId))
    device2.expectMsg(Device.ReadTemperature(reqId))

    queryActor.tell(Device.RespondTemperature(reqId, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(reqId, Some(2.0)), device2.ref)

    requesterActor.expectMsg(DeviceGroup.RespondAllTemperatures(0,
      Map(device1Id -> Temperature(1.0), device2Id -> Temperature(2.0))))
  }

  "return TemperatureNotAvailable from devices with no readings" in {
    val requesterActor = TestProbe()
    val device1        = TestProbe()
    val device2        = TestProbe()
    val device1Id      = UUID.randomUUID()
    val device2Id      = UUID.randomUUID()
    val mapper         = Map(device1.ref -> device1Id, device2.ref -> device2Id)
    val reqId          = 0

    val queryActor = system.actorOf(DeviceGroupQuery.props(mapper, reqId, requesterActor.ref, 3 seconds))
    device1.expectMsg(Device.ReadTemperature(reqId))
    device2.expectMsg(Device.ReadTemperature(reqId))

    queryActor.tell(Device.RespondTemperature(reqId, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(reqId, None), device2.ref)

    requesterActor.expectMsg(DeviceGroup.RespondAllTemperatures(0,
      Map(device1Id -> Temperature(1.0), device2Id -> TemperatureNotAvailable)))
  }

  "return DeviceNotAvailable if device stops answering" in {
    val requesterActor = TestProbe()
    val device1        = TestProbe()
    val device2        = TestProbe()
    val device1Id      = UUID.randomUUID()
    val device2Id      = UUID.randomUUID()
    val mapper         = Map(device1.ref -> device1Id, device2.ref -> device2Id)
    val reqId          = 0

    val queryActor = system.actorOf(DeviceGroupQuery.props(mapper, reqId, requesterActor.ref, 3 seconds))
    device1.expectMsg(Device.ReadTemperature(reqId))
    device2.expectMsg(Device.ReadTemperature(reqId))

    queryActor.tell(Device.RespondTemperature(reqId, Some(1.0)), device1.ref)
    device2.ref ! PoisonPill

    requesterActor.expectMsg(DeviceGroup.RespondAllTemperatures(0,
      Map(device1Id -> Temperature(1.0), device2Id -> DeviceNotAvailable)))
  }

  "return first reply and not be overwritten by second reply" in {
    val requesterActor = TestProbe()
    val device1        = TestProbe()
    val device2        = TestProbe()
    val device1Id      = UUID.randomUUID()
    val device2Id      = UUID.randomUUID()
    val mapper         = Map(device1.ref -> device1Id, device2.ref -> device2Id)
    val reqId          = 0

    val queryActor = system.actorOf(DeviceGroupQuery.props(mapper, reqId, requesterActor.ref, 3 seconds))
    device1.expectMsg(Device.ReadTemperature(reqId))
    device2.expectMsg(Device.ReadTemperature(reqId))

    queryActor.tell(Device.RespondTemperature(reqId, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(reqId, Some(2.0)), device2.ref)
    device2.ref ! PoisonPill

    requesterActor.expectMsg(DeviceGroup.RespondAllTemperatures(0,
      Map(device1Id -> Temperature(1.0), device2Id -> Temperature(2.0))))
  }

  "return DeviceTimedOut if device does not reply in time" in {
    val requesterActor = TestProbe()
    val device1        = TestProbe()
    val device2        = TestProbe()
    val device1Id      = UUID.randomUUID()
    val device2Id      = UUID.randomUUID()
    val mapper         = Map(device1.ref -> device1Id, device2.ref -> device2Id)
    val reqId          = 0

    val queryActor = system.actorOf(DeviceGroupQuery.props(mapper, reqId, requesterActor.ref, 1 seconds))
    device1.expectMsg(Device.ReadTemperature(reqId))
    device2.expectMsg(Device.ReadTemperature(reqId))

    queryActor.tell(Device.RespondTemperature(reqId, Some(1.0)), device1.ref)

    requesterActor.expectMsg(DeviceGroup.RespondAllTemperatures(0,
      Map(device1Id -> Temperature(1.0), device2Id -> DeviceTimedOut)))
  }
}
