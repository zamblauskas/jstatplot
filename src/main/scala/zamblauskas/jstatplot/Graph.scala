package zamblauskas.jstatplot

import java.io.File

import org.sameersingh.scalaplot.Implicits._
import org.sameersingh.scalaplot.Style.{HistogramStyle, Color}
import org.sameersingh.scalaplot.gnuplot.GnuplotPlotter
import org.sameersingh.scalaplot.{BarData, BarSeries, Chart}

import scalaz.\/
import scalaz.effect.IO
import scalaz.syntax.std.boolean._

/**
  * Graph with multiple series.
  */
case class Graph[A](series: Seq[Graph.Series[A]], xAxisLabel: String, yAxisLabel: String)

object Graph {

  /**
    * Series represent a single value from data `A`.
    *
    * @param y extract value for this series from data `A`.
    */
  case class Series[A](y: (A) => Double, name: String, color: Color.Type)

  case class Data[A](timestamp: Double, data: A)

  case class YRange(from: Double, to: Double)

  case class GraphSize(x: Double, y: Double)

  def createChart[A](title: String, range: Option[YRange], graphSize: GraphSize, data: Seq[Data[A]])
                    (implicit graph: Graph[A]): Chart = {

    def bar(p: Series[A]): BarSeries = Bar(
      yp = data.map { d => p.y(d.data) },
      label = p.name,
      color = Some(p.color),
      border = Some(false)
    )

    val barData = new BarData(xAxisName(graphSize.x, data.map(_.timestamp)), graph.series.map(bar))

    barChart(
      barData,
      title = title,
      xLabel = graph.xAxisLabel,
      y = Axis(graph.yAxisLabel, range = range.flatMap(YRange.unapply)),
      showLegend = true,
      legendPosX = org.sameersingh.scalaplot.LegendPosX.Right,
      legendPosY = org.sameersingh.scalaplot.LegendPosY.Top,
      legendPosRegion = org.sameersingh.scalaplot.LegendPosRegion.Outside,
      size = GraphSize.unapply(graphSize),
      style = HistogramStyle.RowStacked
    )
  }

  type Error = String

  /**
    * Create the plot.
    *
    * @param prefix prefix for the resulting graph file name.
    */
  def plot(chart: Chart, prefix: String, destinationDir: File): IO[\/[Error, Unit]] = IO { \/.fromTryCatchNonFatal {
    new GnuplotPlotter(chart).png(destinationDir.getAbsolutePath + "/", prefix)
  }.leftMap(e => s"An error occurred while creating plot '$prefix': '${e.getMessage}'.") }

  private def xAxisName(graphWidth: Double, timestamps: Seq[Double]): Int => String = { idx =>
    val numShow = graphWidth / 96
    val showEvery: Long = if(timestamps.size > numShow) Math.round(timestamps.size / numShow)
                          else                          1

    (idx % showEvery == 0).option(
      timestamps.lift(idx).map(_.toString)
    ).flatten.getOrElse("")
  }

}
