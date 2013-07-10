package be.angelcorp.omicronai

import java.util
import java.util.Comparator
import collection.JavaConverters._
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AStar {

  def heuristic( fromTile: Location): Int

  def costOnto( fromTile: Location, toTile: Location ): Int

  def goalReached( solution: AStarSolution ): Boolean

  def findPath( origin: Location): AStarSolution = {

    val open   = new util.HashMap[Location, AStarSolution]()
    val closed = new util.HashMap[Location, AStarSolution]()

    val originSolution = new AStarSolution(0, heuristic(origin), List(origin))
    open.put( origin, originSolution )

    if ( goalReached( originSolution ) )
      return originSolution

    while ( !open.isEmpty ) {
      val q = open.values().iterator().asScala.minBy( _.f )

      open.remove( q.tile )
      closed.put( q.tile, q )

      for ( target <- q.tile.neighbours ) {
        val g = q.g + costOnto( q.tile, target )
        val h = heuristic(target)
        val targetSubSolution = new AStarSolution(g, h, target :: q.path)

        if ( goalReached(targetSubSolution) )
          return targetSubSolution
        else if ( (!closed.containsKey( target ) || targetSubSolution.f < closed.get( target ).f ) &&
                  (!open.containsKey  ( target ) || targetSubSolution.f < open.get(target).f     ) )
          open.put( target, targetSubSolution )
      }
    }
    throw new IllegalArgumentException("AStar did not find any solution")
  }
}

object AStar{
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def apply( destination: Location ) = new AStar {
    def heuristic(fromTile: Location): Int = {
      //logger.trace( s"Heuristic for tile $fromTile is ${fromTile δ destination}" )
      fromTile δ destination
    }
    def costOnto(fromTile: Location, toTile: Location) = 1
    def goalReached(solution: AStarSolution) = destination == solution.tile
  }

}

class AStarSolution( val g: Int, val h: Int, val path: List[ Location ] ) {

  val f    = g + h
  val tile = path.head

}

