package be.angelcorp.omicron.base.algorithms

import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.world.FieldWorldGraph

object FloodFill {

  def fill( destination: Location ): Field[Double] = {
    val tiles = Field.fill(destination.bounds)(Double.PositiveInfinity)
    val cost  = Field.fill(destination.bounds)(1.0)
    fill(destination, new FieldWorldGraph(tiles, cost, cost, cost, cost))
  }

  def fill( destination: Location, map: FieldWorldGraph[Double, Double] ): Field[Double] = {
    map.tiles(destination) = 0
    fillFromTile(destination, map)
    map.tiles
  }

  def fillFromTile( tile: Location, map: FieldWorldGraph[Double, Double]) {
    val cost = map.tiles(tile)
    for (neighbour <- tile.neighbours) {
        val oldCost = map.tiles(neighbour._2)
        val newCost = cost + map.edgeAt(tile, neighbour._1).getOrElse( Double.MaxValue )
        if (newCost < oldCost && !oldCost.isNaN) {
          map.tiles(neighbour._2) = newCost
          fillFromTile(neighbour._2, map)
      }
    }
  }

}
