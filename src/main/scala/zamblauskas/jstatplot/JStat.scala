package zamblauskas.jstatplot

import com.twitter.conversions.storage._
import com.twitter.util.StorageUnit
import org.sameersingh.scalaplot.Style.Color
import zamblauskas.csv.parser.ColumnBuilder._
import zamblauskas.csv.parser.ColumnReads
import zamblauskas.functional._
import zamblauskas.jstatplot.Graph.Series

case class JStat(
  timestamp: Double,
  capacity: Heap,
  utilization: Heap
)

object JStat {

  implicit val r: ColumnReads[JStat] = (
    column("Timestamp").as[Double] and
    Heap.capacityReads             and
    Heap.utilizationReads
  )(JStat.apply)
}

case class Heap(
  survivor0: StorageUnit,
  survivor1: StorageUnit,
  eden: StorageUnit,
  old: StorageUnit,
  permanent: StorageUnit
)

object Heap {

  val capacityReads: ColumnReads[Heap] = (
    column("S0C").as[Double].map(_.toLong.kilobytes) and
    column("S1C").as[Double].map(_.toLong.kilobytes) and
    column("EC").as[Double].map(_.toLong.kilobytes)  and
    column("OC").as[Double].map(_.toLong.kilobytes)  and
    column("PC").as[Double].map(_.toLong.kilobytes)
  )(Heap.apply)

  val utilizationReads: ColumnReads[Heap] = (
    column("S0U").as[Double].map(_.toLong.kilobytes) and
    column("S1U").as[Double].map(_.toLong.kilobytes) and
    column("EU").as[Double].map(_.toLong.kilobytes)  and
    column("OU").as[Double].map(_.toLong.kilobytes)  and
    column("PU").as[Double].map(_.toLong.kilobytes)
  )(Heap.apply)

  def heapGraph(unitName: String, unitConvert: (StorageUnit) => Double): Graph[Heap] = Graph[Heap](
    series = List(
      Series[Heap](u => unitConvert(u.old), "Old", Color.Red),
      Series[Heap](u => unitConvert(u.eden), "Eden", Color.Blue),
      Series[Heap](u => unitConvert(u.survivor0), "Survivor 1", Color.Green),
      Series[Heap](u => unitConvert(u.survivor1), "Survivor 2", Color.DarkGreen),
      Series[Heap](u => unitConvert(u.permanent), "Permanent", Color.Purple)
    ),
    yAxisLabel = s"Size ($unitName)",
    xAxisLabel = "Timestamp (sec)"
  )
}