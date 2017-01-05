package zamblauskas.jstatplot

import com.twitter.conversions.storage._
import com.twitter.util.StorageUnit
import org.sameersingh.scalaplot.Style.Color
import zamblauskas.csv.parser._
import zamblauskas.functional._
import zamblauskas.jstatplot.Graph.Series

case class JStat[H <: Heap](
  timestamp: Double,
  capacity: H,
  utilization: H,
  gcEvents: NumGcEvents,
  gcTime: GcTime
)

object JStat {
  implicit val jstatJava7Reads: ColumnReads[JStat[HeapJava7]] = (
    column("Timestamp").as[Double] and
    HeapJava7.capacityReads        and
    HeapJava7.utilizationReads     and
    NumGcEvents.numGcEventsReads   and
    GcTime.gcTimeReads
  )(JStat.apply)

  implicit val jstatJava8Reads: ColumnReads[JStat[HeapJava8]] = (
    column("Timestamp").as[Double] and
    HeapJava8.capacityReads        and
    HeapJava8.utilizationReads     and
    NumGcEvents.numGcEventsReads   and
    GcTime.gcTimeReads
  )(JStat.apply)
}

sealed trait Heap

case class HeapJava7(
  survivor0: StorageUnit,
  survivor1: StorageUnit,
  eden: StorageUnit,
  old: StorageUnit,
  permanent: StorageUnit
) extends Heap

case class HeapJava8(
  survivor0: StorageUnit,
  survivor1: StorageUnit,
  eden: StorageUnit,
  old: StorageUnit,
  metaspace: StorageUnit,
  compressed: StorageUnit
) extends Heap

object CsvReads {
  implicit val storageReads: Reads[StorageUnit] = new Reads[StorageUnit] {
    override def read(column: Column): ReadResult[StorageUnit] = {
      // jstat result might contain `-` for columns that don't have value, e.g.
      // when running jstat from Java 8 against application running on Java 7 the
      // "Metaspace" and "Compressed" will be `-`.
      Reads.doubleReads.read(column.copy(value = column.value.replace('-', '0'))).map(_.toLong.kilobytes)
    }
  }
}

object HeapJava7 {

  import CsvReads._

  val capacityReads: ColumnReads[HeapJava7] = (
    column("S0C").as[StorageUnit] and
    column("S1C").as[StorageUnit] and
    column("EC").as[StorageUnit]  and
    column("OC").as[StorageUnit]  and
    column("PC").as[StorageUnit]
  )(HeapJava7.apply)

  val utilizationReads: ColumnReads[HeapJava7] = (
    column("S0U").as[StorageUnit] and
    column("S1U").as[StorageUnit] and
    column("EU").as[StorageUnit]  and
    column("OU").as[StorageUnit]  and
    column("PU").as[StorageUnit]
  )(HeapJava7.apply)

  def heapGraph(unitName: String, unitConvert: (StorageUnit) => Double): Graph[HeapJava7] = Graph[HeapJava7](
    series = List(
      Series[HeapJava7](u => unitConvert(u.old), "Old", Color.Red),
      Series[HeapJava7](u => unitConvert(u.eden), "Eden", Color.Blue),
      Series[HeapJava7](u => unitConvert(u.survivor0), "Survivor 1", Color.Green),
      Series[HeapJava7](u => unitConvert(u.survivor1), "Survivor 2", Color.DarkGreen),
      Series[HeapJava7](u => unitConvert(u.permanent), "Permanent", Color.Purple)
    ),
    yAxisLabel = s"Size ($unitName)",
    xAxisLabel = "Timestamp (sec)"
  )
}

object HeapJava8 {

  import CsvReads._

  val capacityReads: ColumnReads[HeapJava8] = (
    column("S0C").as[StorageUnit] and
    column("S1C").as[StorageUnit] and
    column("EC").as[StorageUnit]  and
    column("OC").as[StorageUnit]  and
    column("MC").as[StorageUnit]  and
    column("CCSC").as[StorageUnit]
  )(HeapJava8.apply)

  val utilizationReads: ColumnReads[HeapJava8] = (
    column("S0U").as[StorageUnit] and
    column("S1U").as[StorageUnit] and
    column("EU").as[StorageUnit]  and
    column("OU").as[StorageUnit]  and
    column("MU").as[StorageUnit]  and
    column("CCSU").as[StorageUnit]
  )(HeapJava8.apply)

  def heapGraph(unitName: String, unitConvert: (StorageUnit) => Double): Graph[HeapJava8] = Graph[HeapJava8](
    series = List(
      Series[HeapJava8](u => unitConvert(u.old), "Old", Color.Red),
      Series[HeapJava8](u => unitConvert(u.eden), "Eden", Color.Blue),
      Series[HeapJava8](u => unitConvert(u.survivor0), "Survivor 1", Color.Green),
      Series[HeapJava8](u => unitConvert(u.survivor1), "Survivor 2", Color.DarkGreen),
      Series[HeapJava8](u => unitConvert(u.metaspace), "Metaspace", Color.Purple),
      Series[HeapJava8](u => unitConvert(u.compressed), "Compressed", Color.Maroon)
    ),
    yAxisLabel = s"Size ($unitName)",
    xAxisLabel = "Timestamp (sec)"
  )
}

case class NumGcEvents(
  young: Int,
  full: Int
)

object NumGcEvents {
  val numGcEventsReads: ColumnReads[NumGcEvents] = (
    column("YGC").as[Int] and
    column("FGC").as[Int]
  )(NumGcEvents.apply)

  implicit val numGcEventsGraph: Graph[NumGcEvents] = Graph[NumGcEvents](
    series = List(
      Series[NumGcEvents](_.full.toDouble, "Full", Color.Red),
      Series[NumGcEvents](_.young.toDouble, "Young", Color.Blue)
    ),
    yAxisLabel = "Number of events",
    xAxisLabel = "Timestamp (sec)"
  )
}

case class GcTime(
  young: Double,
  full: Double
)

object GcTime {
  val gcTimeReads: ColumnReads[GcTime] = (
    column("YGCT").as[Double] and
    column("FGCT").as[Double]
  )(GcTime.apply)

  implicit val gcTimeGraph: Graph[GcTime] = Graph[GcTime](
    series = List(
      Series[GcTime](_.full, "Full", Color.Red),
      Series[GcTime](_.young, "Young", Color.Blue)
    ),
    yAxisLabel = "Time (sec)",
    xAxisLabel = "Timestamp (sec)"
  )
}
