package be.angelcorp.omicronai.world

import scala.Some
import java.util.Comparator
import akka.actor.{Props, ActorSystem, Actor}
import akka.dispatch.{Envelope, UnboundedPriorityMailbox}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.typesafe.config.Config
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.algorithms.Field
import be.angelcorp.omicronai.ai.pike.agents._
import be.angelcorp.omicronai.ai.pike.agents.TileContentsChanged
import be.angelcorp.omicronai.ai.pike.agents.TileResourcesChanged
import be.angelcorp.omicronai.ai.pike.agents.UnitMoved

class World(player: Player, sz: WorldSize) extends Actor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  private implicit val game = player.getController.getGameController.getGame
  private val gameState = Field.fill[WorldState](sz)(UnknownState)

  override def preStart() {
    val events = List(
      classOf[TileContentsChanged],
      classOf[TileResourcesChanged],
      classOf[UnitMoved],
      classOf[PlayerGainedObject],
      classOf[PlayerLostObject]
    )
    events.foreach( event => context.system.eventStream.subscribe(self, event) )
  }

  private def checkResources(t: Tile) =
    ResourceType.values().map( r => {
      val q = t.checkResourceQuantity( r )
      (r, q.presence() match {
        case Presence.PRESENT => q.get().intValue()
        case _ => 0
      } )
    } ).toMap

  // Current state as known bu the game
  private def realState(l: Location) = {
    val t = Location.location2tile(l)
    val content   = t.checkContents()
    content.presence() match {
      case Presence.PRESENT => KnownState(l, Some(content.get), checkResources(t))
      case Presence.ABSENT  => KnownState(l, None, checkResources(t))
      case Presence.UNKNOWN => UnknownState
    }
  }

  private def updateLocation(l: Location) {
    //logger.trace(s"Updating world state on $l")
    realState(l) match {
      // If we can see the tile, update the map state
      case newState: KnownState =>
        gameState(l) = newState
      // If we could see the tile, but not anymore, make it a ghost tile
      case UnknownState => gameState(l) match {
        case current: KnownState => gameState(l) = current.toGhost
        case _ => // Do nothing, already ghost or unknown
      }
      case _ => throw new UnsupportedOperationException // Ghost state is never returned
    }
  }

  def getState(l: Location): WorldState = gameState(l)

  def receive: Actor.Receive = {
    case ReloadLocation(l) => updateLocation(l)
    case ReloadReady()     => sender ! true // True due to priority handling of messages
    case LocationState(l)  => sender ! getState(l)
    case LocationStates(l) => sender ! l.map( loc => getState(loc) )

    case m: GameListenerMessage => processEvent(m)
  }

  private def viewRange(gameObject: IGameObject) =
    gameObject.getModule(PublicModuleType.BASE, 0).get().getViewRange

  def processEvent(m: GameListenerMessage) = m match {
    case TileContentsChanged(l, _)      => updateLocation(l)
    case TileResourcesChanged(l, _, _)  => updateLocation(l)

    case UnitMoved(gameObject, location) => {
      val vr = viewRange(gameObject)

      val couldSee = (location.getFrom: Location).range( vr )
      val canSee   = (location.getTo:   Location).range( vr )
      val visibleToInvisible = couldSee.diff(canSee)
      val invisibleToVisible = canSee.diff(couldSee)
      visibleToInvisible.foreach( updateLocation )
      invisibleToVisible.foreach( updateLocation )
    }

    case PlayerGainedObject(_, obj) =>
      val tile = obj.checkLocation()
      tile.presence() match {
        case Presence.PRESENT => (tile.get: Location).range( viewRange(obj) ).foreach( l => updateLocation(l) )
        case _ =>
      }

    case PlayerLostObject(_, obj) =>
      val tile = obj.checkLocation()
      tile.presence() match {
        case Presence.PRESENT => (tile.get: Location).range( viewRange(obj) ).foreach( l => updateLocation(l) )
        case _ =>
      }
    case m =>
      logger.info(s"No world update action for $m")
  }

}

object World {

  def apply( player: Player, size: WorldSize ) =
    Props(classOf[World], player, size).withDispatcher("akka.world-dispatcher")

  def withInterface(player: Player, size: WorldSize) = {
    val system = ActorSystem("WorldActorSystem")
    val world = system.actorOf( World(player, size) )
    new WorldInterface(world)
  }

}

sealed abstract class WorldActorMsg
private case class GetWorldListener()               extends WorldActorMsg
private case class ReloadLocation(l: Location)      extends WorldActorMsg
case class ReloadReady()                            extends WorldActorMsg
case class LocationState (l: Location)              extends WorldActorMsg
case class LocationStates(l: Seq[Location])         extends WorldActorMsg

class PrioritizedMailbox(settings: ActorSystem.Settings, cfg: Config) extends UnboundedPriorityMailbox ( new Comparator[Envelope] {

  def priority: PartialFunction[Any, Int] = {
    case Envelope(m: GameListenerMessage, _) => 0
    case Envelope(m: ReloadLocation     , _) => 1
    case Envelope(m: ReloadReady        , _) => 3
    case Envelope(m: LocationState      , _) => 6
    case Envelope(m: LocationStates     , _) => 6
    case _                                   => 6
  }

  override def compare(thisMessage: Envelope, thatMessage: Envelope): Int =
    priority(thisMessage) - priority(thatMessage)

} )

sealed abstract class WorldState
object UnknownState   extends WorldState
case class GhostState(location: Location, content: Option[IGameObject], resources: Map[ResourceType, Int]) extends WorldState
case class KnownState(location: Location, content: Option[IGameObject], resources: Map[ResourceType, Int]) extends WorldState {
  def toGhost = GhostState(location, content, resources)
}

