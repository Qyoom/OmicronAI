package be.angelcorp.omicron.base.gui.input

import de.lessvoid.nifty.slick2d.input.AbstractSlickInputSystem
import de.lessvoid.nifty.slick2d.input.events._
import org.lwjgl.input.Keyboard._
import be.angelcorp.omicron.base.util.GenericEventBus

class InputSystem(val eventStream: GenericEventBus) extends AbstractSlickInputSystem {

  def isAltDown = isKeyDown( KEY_LMENU ) || isKeyDown( KEY_RMENU )

  /**
   * Publish the input event on the EventStream
   */
  def handleInputEvent(niftyEvent: InputEvent) {
    eventStream.publish(
      niftyEvent match {
        case e: KeyboardEventReleased => KeyReleased(e.getKey, e.isShiftDown, e.isControlDown, isAltDown)
        case e: KeyboardEventPressed  => KeyPressed(e.getKey, e.isShiftDown, e.isControlDown, isAltDown)
        case e: MouseEventWheelMoved  => MouseWheelMoved(e.getX, e.getY, e.getDelta)
        case e: MouseEventPressed     => MousePressed( e.getX, e.getY, e.getButton)
        case e: MouseEventDragged     => MouseDragged( e.getX, e.getY, e.getTargetX, e.getTargetY, e.getButton)
        case e: MouseEventClicked     => MouseClicked( e.getX, e.getY, e.getButton, e.getCount)
        case e: MouseEventReleased    => MouseReleased(e.getX, e.getY, e.getButton)
        case e: MouseEventMoved       => MouseMoved(e.getX, e.getY, e.getTargetX, e.getTargetY)
      }
    )
  }

}

sealed abstract class GuiInputEvent
case class KeyReleased(keyId: Int, shiftDown: Boolean, controlDown: Boolean, altDown: Boolean) extends  GuiInputEvent
case class KeyPressed( keyId: Int, shiftDown: Boolean, controlDown: Boolean, altDown: Boolean) extends  GuiInputEvent
case class MouseWheelMoved(mouseX: Int, mouseY: Int, delta: Int) extends  GuiInputEvent
case class MousePressed(x: Int, y: Int, mouseButton: Int) extends  GuiInputEvent
case class MouseDragged(startX: Int, startY: Int, endX: Int, endY: Int, mouseButton: Int) extends  GuiInputEvent
case class MouseClicked(x: Int, y: Int, mouseButton: Int, clickCount: Int) extends  GuiInputEvent
case class MouseReleased(x: Int, y: Int, mouseButton: Int) extends  GuiInputEvent
case class MouseMoved(startX: Int, startY: Int, endX: Int, endY: Int) extends  GuiInputEvent
