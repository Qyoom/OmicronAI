package be.angelcorp.omicronai.gui.layerRender

import scala.concurrent.{TimeoutException, Await}
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.newdawn.slick.{Graphics, Color}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.IGameObject
import be.angelcorp.omicronai.gui.{ViewPort, Canvas}
import be.angelcorp.omicronai.{Location, HexTile}
import be.angelcorp.omicronai.gui.textures.MapIcons
import be.angelcorp.omicronai.world._
import be.angelcorp.omicronai.gui.slick.DrawStyle
import be.angelcorp.omicronai.world.KnownState
import be.angelcorp.omicronai.world.GhostState
import scala.Some
import be.angelcorp.omicronai.world.LocationStates

class ObjectLayer(world:  ActorRef,
                  filter: IGameObject => Boolean,
                  name:   String,
                  knownFill: Color  = Color.green,
                  ghostFill: Color  = Color.lightGray,
                  border: DrawStyle = Color.transparent) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = 50 milliseconds;

  import scala.concurrent.ExecutionContext.Implicits.global

  // { object, location, isGhost }
  var objects = Array.ofDim[(IGameObject, Location, Boolean)](0)

  override def prepareRender(subWorld: SubWorld, layer: Int) {
    objects = subWorld.states(layer).map( {
        case (loc, KnownState(_, optionContent, _)) => optionContent match {
          case Some(obj) if filter(obj) => Some((obj, loc, false))
          case _ => None
        }
        case (loc, GhostState(_, optionContent, _)) => optionContent match {
          case Some(obj) if filter(obj) => Some((obj, loc, true))
          case _ => None
        }
        case _          => None
      } ).flatten
  }

  override def render(g: Graphics) {
    objects.foreach( entry => {
      val tile: HexTile = entry._2
      new Canvas( tile ) {
        override def borderStyle: DrawStyle = border
        override def fillColor: Color       = if (entry._3) ghostFill else knownFill
      }.render(g)
      val (centerX, centerY) = Canvas.center( tile )
      MapIcons.getIcon( entry._1 ).drawCentered( centerX, centerY )
    } )
  }

  override def toString = name
}
