package be.angelcorp.omicron.base.gui.layerRender

import org.newdawn.slick.Graphics
import be.angelcorp.omicron.base.gui.ViewPort
import be.angelcorp.omicron.base.world.SubWorld

trait LayerRenderer {

  /**
   * Called before the render method when the viewport has changed (position or scale)
   */
  def viewChanged(view: ViewPort) { }

  def prepareRender(subWorld: SubWorld, layer: Int)

  def render(g: Graphics)

  override def toString: String

}

object LayerRenderer {

  def apply() = new LayerRenderer{
    override def prepareRender(subWorld: SubWorld, layer: Int) {}
    override def render(g: Graphics) = {}
  }

}
