package zamblauskas.jstatplot

import java.io.File

import zamblauskas.csv.parser.{ColumnReads, Parser}
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
      val destination = jstatResult.getAbsoluteFile.getParentFile
      Graph.plot(chart, name, destination)
    }

    def parse[H <: Heap](csv: String)(implicit cr: ColumnReads[JStat[H]]): IO[String \/ Seq[JStat[H]]] = IO { \/.fromEither {
      Parser.parse[JStat[H]](csv)
        .leftMap(f => s"Parsing '${jstatResult.getAbsolutePath}' failed with '${f.message}' on line ${f.lineNum}.")
    }}

    implicit val heapJava7Graph: Graph[HeapJava7] = HeapJava7.heapGraph(sizeUnit.name, sizeUnit.extract)
    implicit val heapJava8Graph: Graph[HeapJava8] = HeapJava8.heapGraph(sizeUnit.name, sizeUnit.extract)

    def plotJstat[H <: Heap](csv: String)(implicit g: Graph[H], cr: ColumnReads[JStat[H]]): EitherT[IO, String, Unit] = {
      for {
        lines <- eitherT(parse(csv))
        _     <- eitherT(plot("Heap Capacity", lines.map(j => Data(j.timestamp, j.capacity))))
        _     <- eitherT(plot("Heap Utilization", lines.map(j => Data(j.timestamp, j.utilization))))
        _     <- eitherT(plot("Number of GC events", lines.map(j => Data(j.timestamp, j.gcEvents))))
        _     <- eitherT(plot("GC time", lines.map(j => Data(j.timestamp, j.gcTime))))
      } yield ()
    }

    def error(msg: String): EitherT[IO, String, Unit] = eitherT(IO{msg.left[Unit]})

    (for {
      csv    <- eitherT(Reader.readAsCsvString(jstatResult, skipNumLines))
      _      <- if(Parser.isHeaderValid[JStat[HeapJava7]](csv))      plotJstat[HeapJava7](csv)
                else if(Parser.isHeaderValid[JStat[HeapJava8]](csv)) plotJstat[HeapJava8](csv)
                else                                                 error(s"Invalid result file ${jstatResult.getAbsolutePath}")
    } yield ()).run
  }
}
