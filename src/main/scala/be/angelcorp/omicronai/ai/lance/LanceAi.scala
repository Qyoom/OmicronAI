package be.angelcorp.omicronai.ai.lance

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.GameListener
import com.lyndir.omicron.api.model.{Security, Game, Color, PlayerKey}
import com.lyndir.omicron.api.model.Color.Template._
import be.angelcorp.omicronai.Settings._
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.gui.layerRender.{LayerRenderer, GridRenderer}
import org.newdawn.slick.Graphics
import be.angelcorp.omicronai.gui.input._
import be.angelcorp.omicronai.HexTile
import scala.Some

class LanceAi( playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  Security.authenticate(this, key)

  def this( builder: Game.Builder) =
    this( builder.nextPlayerID, new PlayerKey, settings.ai.name, RED.get )


  def gameListener: GameListener = new GameListener {
    
  }

  def buildGuiInterface(gui: AiGui, nifty: Nifty): GuiInterface = new GuiInterface {
    nifty.addScreen( screens.Introduction.name, screens.Introduction.screen(nifty, gui) )
    nifty.addScreen( screens.ui.UserInterface.name, screens.ui.lance.LanceUserInterface.screen(nifty, gui) )

    nifty.gotoScreen( screens.Introduction.name )

    var fromTile: Option[HexTile] = None
    var toTile: Option[HexTile] = None

    gui.input.inputHandlers.prepend( new InputHandler {
      var lastX: Int = -1
      var lastY: Int = -1
      def handleInputEvent(event: GuiInputEvent): Boolean = event match {
        case MousePressed(x, y, 0)  => lastX = x; lastY = y; false
        case MouseReleased(x, y, 0) if x == lastX && y == lastY =>
          fromTile = toTile
          val co = gui.view.pixelToOpengl(x, y)
          toTile = Some( gui.view.openglToTile( co._1, co._2 ) )
          logger.info( s"FromTile = $fromTile \t | toTile = $toTile" )
          true
        case _ => false
      }
    } )

    activeLayers += new GridRenderer(LanceAi.this)
    activeLayers += new LayerRenderer {
      val  sz = gui.game.getLevelSize
      def render(g: Graphics, view: ViewPort) {
        for (from <- fromTile; to <- toTile) {
          g.setColor( org.newdawn.slick.Color.magenta )
          g.drawLine( from.centerXY._1* GuiTile.scale, from.centerXY._2* GuiTile.scale, to.centerXY._1* GuiTile.scale, to.centerXY._2 * GuiTile.scale)
        }
      }
    }

  }

}
