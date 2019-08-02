//package hotelpricedrops.notifier.grapher
//
//import java.time.Instant
//
//import cats.effect.IO
//import scalax.chart.api._
//
//trait Grapher {
//  def plotGraph: IO[Unit]
//}
//
//object Grapher extends App {
//
//  def apply(data: List[(Int, Int)]) = {
//    val chart = XYLineChart(data)
//    chart.saveAsPNG("/tmp/chart.png")
//  }
//
//  val testData = List((1, 5), (2, 10))
//  apply(testData)
//}
