package be.angelcorp.omicronai.world

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import be.angelcorp.omicronai.Location

class WorldInterface(val worldRef: ActorRef) {

  def stateOf(l: Location)(implicit timeout: Timeout = Timeout(Duration(10, TimeUnit.SECONDS))) =
    ask(worldRef, LocationState(l)).mapTo[WorldState]

  def statesOf(l: Seq[Location])(implicit timeout: Timeout = Timeout(Duration(10, TimeUnit.SECONDS))) =
    ask(worldRef, LocationStates(l)).mapTo[Seq[WorldState]]

  def isReady(implicit timeout: Timeout = Timeout(Duration(1, TimeUnit.MINUTES))) =
    ask(worldRef, ReloadReady()).mapTo[Boolean]

  lazy val listener = new WorldUpdater( worldRef )

}