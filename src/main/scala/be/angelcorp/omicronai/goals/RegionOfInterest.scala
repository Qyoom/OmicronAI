package be.angelcorp.omicronai.goals

import math._
import be.angelcorp.omicronai.Location
import scala.annotation.tailrec

trait RegionOfInterest {

  def inArea( l: Location ): Boolean

  def tiles: Seq[Location]

  def size: Int = tiles.size

  def center: Location

}

class SquareArea(val lowerBound: Location, val upperBound: Location) extends RegionOfInterest {

  val sizeU = lowerBound δu upperBound
  val sizeV = lowerBound δv upperBound
  val sizeH = lowerBound δh upperBound

  val center = lowerBound Δ ( sizeU / 2, sizeV / 2, sizeH / 2 )

  def inArea(tile: Location) = {
    val du = lowerBound δu tile
    val dv = lowerBound δv tile
    val dh = lowerBound δh tile

    du > 0 && dv > 0 && dh > 0&& du <= sizeU && dv <= sizeV && dh <= sizeH
  }

  def tiles: Seq[Location] =
    for (du <- 0 to (lowerBound δu upperBound);
         dv <- 0 to (lowerBound δv upperBound);
         dh <- 0 to (lowerBound δh upperBound)) yield lowerBound Δ (du, dv, dh)

}

class HexArea(val center: Location, val radius: Int) extends RegionOfInterest {
  require( radius >= 0 )

  def inArea(l: Location) = abs( center δ l ) <= radius

  lazy val tiles = grow(Seq(center), radius)

  @tailrec
  private def grow( tiles: Seq[Location], growTimes: Int ): Seq[Location] =
    if (growTimes <= 0)
      tiles
    else
      grow(
        (for ( tile <- tiles ) yield tile :: tile.neighbours ).flatten.distinct,
        growTimes - 1
      )

}


