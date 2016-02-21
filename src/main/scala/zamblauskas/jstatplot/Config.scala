package zamblauskas.jstatplot

import java.io.File

import com.twitter.util.StorageUnit
import scopt.OptionParser

/**
  * Size unit to present in the graph.
  * @param name unit name
  * @param extract extract the numeric value to display.
  */
sealed abstract class SizeUnit(val name: String, val extract: (StorageUnit) => Double)
object SizeUnit {
  val units: Seq[SizeUnit] = Seq(MB, GB)

  def fromName(n: String): Option[SizeUnit] = units.find(_.name == n)
  def names: Seq[String] = units.map(_.name)
}
case object MB extends SizeUnit("MB", _.inMegabytes)
case object GB extends SizeUnit("GB", _.inGigabytes)

/**
  * CLI configuration params.
  */
case class Config(
   jstatResults: List[File] = List.empty,
   graphWidth: Int = 720,
   graphHeight: Int = 480,
   skipNumLines: Int = 0,
   sizeUnit: SizeUnit = MB,
   graphRangeYTo: Option[Int] = None
)

object Config {
  val parser = new OptionParser[Config]("jstatplot") {
    arg[File]("<file>...").unbounded.action { (f, c) =>
      c.copy(jstatResults = c.jstatResults :+ f)
    }.text("One or more of jstat result files.")

    opt[String]('u', "unit").action { (u, c) =>
      SizeUnit.fromName(u) match {
        case Some(unit) => c.copy(sizeUnit = unit)
        case None       => c
      }
    }.validate { u =>
      if(SizeUnit.fromName(u).isDefined) success
      else failure(s"'unit' must be one of [${SizeUnit.names.mkString(", ")}]")
    }.text(s"Size unit to display in graphs. Available values are [${SizeUnit.names.mkString(", ")}].")

    opt[Int]('w', "width").action { (w, c) =>
      c.copy(graphWidth = w)
    }.validate { w =>
      if(w > 0) success
      else failure("'width' should be > 0")
    }.text("Graph width")

    opt[Int]('h', "height").action { (h, c) =>
      c.copy(graphHeight = h)
    }.validate { h =>
      if(h > 0) success
      else failure("'height' should be > 0")
    }.text("Graph height")

    opt[Int]('y',"range-y").action { (y, c) =>
      c.copy(graphRangeYTo = Some(y))
    }.validate { y =>
      if(y > 0) success
      else failure("'range-y' should be > 0")
    }.text("Fix Y axis range upper value. " +
           "By default max value in data will be used.")

    opt[Int]('s', "skip").action { (s, c) =>
      c.copy(skipNumLines = s)
    }.validate { s =>
      if(s >= 0) success
      else failure("'skip' should be >= 0")
    }.text("Number of lines to skip before actual jstat result begins. " +
           "You should use this option if you write any additional information at the " +
           "beginning of jstat result file (e.g. start time, configuration parameters).")
  }
}