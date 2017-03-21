package zamblauskas.jstatplot

import java.io.File

import org.apache.commons.lang3.exception.ExceptionUtils

import scala.io.Source
import scalaz.\/
import scalaz.effect.IO

object Reader {

  type Error = String

  /**
    * Read jstat result file and convert it to a CSV format.
    */
  def readAsCsvString(f: File, skip: Int): IO[Error \/ String] = IO { \/.fromTryCatchNonFatal {
    Source.fromFile(f)
      .getLines
      .drop(skip)
      .map(_.trim.replaceAll("\\s+", ","))
      .mkString("\n")
  }.leftMap(t => s"Cannot read file '${f.getAbsolutePath}'\n${ExceptionUtils.getStackTrace(t)}") }
}
