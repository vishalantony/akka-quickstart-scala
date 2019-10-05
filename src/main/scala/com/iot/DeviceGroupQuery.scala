package com.iot

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated }
import com.iot.DeviceGroup.DeviceTimedOut

import scala.concurrent.duration.FiniteDuration

object DeviceGroupQuery {

  case object CollectionTimeout

  def props(
             actorToDeviceId: Map[ActorRef, UUID],
             requestId: Long,
             requester: ActorRef,
             timeout: FiniteDuration
           ): Props =
    Props(new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout))
}

/**
 * DeviceGroupQuery is an actor which performs the querying of all devices belonging
 * to a particular device group.
 *
 * @param actorToDeviceId maps device actor references to the device id (UUID)
 * @param requestId       ID of the request which the device group receives.
 * @param requester       The device group which wants to query all the child devices.
 * @param timeout         Duration for which the query should wait for all devices to respond.
 */

class DeviceGroupQuery(
                        actorToDeviceId: Map[ActorRef, UUID],
                        requestId: Long,
                        requester: ActorRef,
                        timeout: FiniteDuration
                      ) extends Actor with ActorLogging {

  import DeviceGroupQuery._
  import context.dispatcher

  // Will send the actor `self` a message(CollectionTimeout) after a duration of `timeout`
  val queryTimeoutTimer: Cancellable =
    context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)


  def receivedResponse(
                        deviceActor: ActorRef,
                        stillWaiting: Set[ActorRef],
                        repliesSoFar: Map[UUID, DeviceGroup.TemperatureReading],
                        response: DeviceGroup.TemperatureReading
                      ): Unit = {
    // Once a response is received from an actor, stop watching it to avoid overwriting values.
    context.unwatch(deviceActor)
    val deviceId            = actorToDeviceId(deviceActor)
    val currentStillWaiting = stillWaiting - deviceActor
    val newReplies          = repliesSoFar + (deviceId -> response)
    if (currentStillWaiting.isEmpty) {
      // No more devices to wait. All have replied with their temperature readings.
      requester ! DeviceGroup.RespondAllTemperatures(requestId, newReplies)
      context.stop(self) // Stop the query actor. This will cancel the timer (see postStop)
    } else {
      // More devices need to respond with their temperature.
      // This replaces the receive with the new partial function where `stillWaiting` and `repliesSoFar`
      // are updated.
      // This replaces the actors message loop/implementation at runtime.
      context.become(waitingForReplies(newReplies, currentStillWaiting))
    }
  }


  def waitingForReplies(
                         repliesSoFar: Map[UUID, DeviceGroup.TemperatureReading],
                         stillWaiting: Set[ActorRef]
                       ): Receive = {
    case Device.RespondTemperature(`requestId`, value) =>
      val deviceActor = sender()
      val response    = value match {
        case None => DeviceGroup.TemperatureNotAvailable
        case Some(temperature) => DeviceGroup.Temperature(temperature)
      }
      receivedResponse(deviceActor, stillWaiting, repliesSoFar, response)


    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, stillWaiting, repliesSoFar, DeviceGroup.DeviceNotAvailable)

    case CollectionTimeout =>
      // Query timed out. Respond all temperatures and stop the actor.
      // Whichever actor did not respond with temperature readings, give
      // the default response `DeviceTimedOut`
      val timedOutResponses = stillWaiting.map { actor =>
        val deviceId = actorToDeviceId(actor)
        deviceId -> DeviceTimedOut
      }
      requester ! DeviceGroup.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutResponses)
      context.stop(self)
  }


  override def receive: Receive =
    waitingForReplies(Map.empty[UUID, DeviceGroup.TemperatureReading], actorToDeviceId.keySet)

  override def preStart(): Unit =
    // Get all devices and read their temperature.
    actorToDeviceId.keysIterator.foreach { deviceActor =>
      context.watch(deviceActor)
      deviceActor ! Device.ReadTemperature(requestId)
    }

  override def postStop(): Unit =
    queryTimeoutTimer.cancel() // Cancel the timer when the actor stops.
}
