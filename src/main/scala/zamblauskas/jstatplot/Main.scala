package zamblauskas.jstatplot

import java.io.File

import zamblauskas.csv.parser.Parser
import zamblauskas.jstatplot.Graph.{Data, GraphSize, YRange}

import scalaz.Scalaz._
import scalaz._
import scalaz.effect._
import scalaz.EitherT.eitherT

object Main {

  def main(args: Array[String]) {
    Config.parser.parse(args, Config()).foreach { config =>
      val graphRange = config.graphRangeYTo.map(YRange(0, _))
      val graphSize = GraphSize(config.graphWidth, config.graphHeight)
      val results = config.jstatResults.map { file =>
        for {
          result <- createGraph(file, config.skipNumLines, graphRange, graphSize, config.sizeUnit)
          output <- result match {
            case -\/(err) => IO.putStrLn(err)
            case \/-(())  => IO.putStrLn(s"Plotted '$file'.")
          }
        } yield output
      }.sequenceU
      results.unsafePerformIO()
    }
  }

  private def createGraph(jstatResult: File,
                          skipNumLines: Int,
                          yRange: Option[YRange],
                          size: GraphSize,
                          sizeUnit: SizeUnit): IO[String \/ Unit] = {
    def plot[A](title: String, h: Seq[Data[A]])(implicit g: Graph[A]): IO[String \/ Unit] = {
      val chart = Graph.createChart(title, yRange, size, h)
      val name = jstatResult.getName + "-" + title.toLowerCase.replaceAll("\\s+", "-")
      val destination = jstatResult.getParentFile
      Graph.plot(chart, name, destination)
    }

    def parse(csv: String): IO[String \/ Seq[JStat]] = IO { \/.fromEither {
      Parser.parse[JStat](csv)
        .leftMap(f => s"Parsing '${jstatResult.getAbsolutePath}' failed with '${f.message}' on line ${f.lineNum}.")
    }}

    implicit val heapGraph = Heap.heapGraph(sizeUnit.name, sizeUnit.extract)

    (for {
      csv   <- eitherT(Reader.readAsCsvString(jstatResult, skipNumLines))
      jstat <- eitherT(parse(csv))
      _     <- eitherT(plot("Heap Capacity", jstat.map(j => Data(j.timestamp, j.capacity))))
      _     <- eitherT(plot("Heap Utilization", jstat.map(j => Data(j.timestamp, j.utilization))))
    } yield ()).run
  }
}
