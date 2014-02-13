package be.angelcorp.omicronai.ai.noai.gui.screens

import scala.Some
import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.ai.actions.{ConstructionAssistAction, AttackAction, MoveAction}
import be.angelcorp.omicronai.{DOWN, UP}
import be.angelcorp.omicronai.gui.nifty.PopupController

class NoAiPopupController(ui: NoAiUserInterfaceController) extends PopupController {

  /** Nifty gui */
  override def nifty: Nifty = ui.nifty

  override def gui = ui.gui.frame.frame

  /** Generates the content for the default right-click popup menu  */
  override def defaultMenu = {
    val entries = List.newBuilder[(String, () => Unit)]
    val location = ui.gui.frame.hoverLocation
    ui.gui.noai.selected match {
      case Some(asset) =>
        // Module related actions
        for (module <- asset.modules) {
          module match {
            case base: BaseModule =>
            case constr: ConstructorModule if !entries.result().exists( _._1 == "Build") =>
              entries += ("Build", () => location match {
                case Some(target) => ui.gui.gotoConstructionScreen( asset, target )
                case _ =>  logger.info(s"Cannot open build menu for $asset, not hovering over any target build tile!")
              } )
            case cont: ContainerModule =>
            case extr: ExtractorModule =>
            case move: MobilityModule  =>
              location match {
                case Some(l) =>
                  // Move in the same level
                  entries += ( "Move to", () => ui.gui.noai.updateOrConfirmAction(MoveAction(asset, l, ui.gui.noai.world)) )
                  // Move up or down
                  implicit val game = ui.gui.frame.game
                  for (d <- Seq(UP(), DOWN()); loc <- l.neighbour(d)) {
                    if (asset.costForLevelingToLevel(loc.h) != Double.MaxValue)
                      entries += (s"Move ${d.toString.toLowerCase}", () => ui.gui.noai.updateOrConfirmAction( MoveAction(asset, loc, ui.gui.noai.world) ))
                  }
                case _ => logger.info(s"Cannot move $asset to that tile, not hovering over any tile!")
              }
            case weapon: WeaponModule =>
              entries += ( s"Fire at (${weapon.getAmmunition}/${weapon.getAmmunitionLoad})", () => { location match  {
                case Some(destination) => ui.gui.noai.updateOrConfirmAction( AttackAction(asset, weapon, destination) )
                case _ => logger.info("Cannot shoot at that tile, not hovering over any tile!")
              } } )
            case _ => // Already in list or no action known
          }
        }
        // Right click on unit
        location.map( hover => ui.gui.noai.unitOn( hover ).map( hoverAsset => {
          hoverAsset.gameObject.getType match {
            case UnitTypes.CONSTRUCTION =>
              entries += ("Assist construction", () => ui.gui.noai.updateOrConfirmAction( ConstructionAssistAction(asset, hover, ui.gui.noai.world) ) )
            case _ =>
          }
        } ) )
      case _ =>
    }
    entries.result()
  }

}
