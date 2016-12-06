package io.chumps.flink

import java.io.IOException
import java.net.{ DatagramPacket, DatagramSocket, InetSocketAddress, SocketException }
import java.util.ConcurrentModificationException
import scala.collection.JavaConversions._

import org.apache.flink.metrics._
import org.apache.flink.metrics.reporter.{AbstractReporter, Scheduled}
import org.slf4j.LoggerFactory

object ReporterFunctions {
  sealed trait MetricType
  object MetricType {
    object Counter extends MetricType
    object Gauge extends MetricType
    object Histogram extends MetricType

    def string(t: MetricType) = t match {
      case Counter => "c"
      case Gauge => "g"
      case Histogram => "h"
    }
  }

  val LOG = LoggerFactory.getLogger(getClass)

  val DefaultMetricName = "metric"

  def extract(pattern: String, label: String): (String, Map[String, String]) = {
    val fragments = label.split('.')
    val tags = pattern.split('.').zip(fragments).toMap
    val metricName = fragments.drop(tags.size).mkString(".")

    (if (metricName.isEmpty) DefaultMetricName else metricName, tags)
  }

  def datagram(prefix: String, pattern: String, label: String, value: Any, `type`: MetricType): Array[Byte] = {
    val (name, tags) = extract(pattern, label)
    val tagsString = tags.map({ case (k, v) => s"#$k:$v" }).mkString(",")
    val datagramString = s"$prefix.$name:$value|${MetricType.string(`type`)}|$tagsString"

    datagramString.getBytes
  }

  def gaugeDatagram(prefix: String, pattern: String, gauge: Gauge[_], label: String): Option[Array[Byte]] =
    Option(gauge.getValue).map(datagram(prefix, pattern, label, _, MetricType.Gauge))

  def counterDatagram(prefix: String, pattern: String, counter: Counter, label: String): Array[Byte] =
    datagram(prefix, pattern, label, counter.getCount, MetricType.Counter)

  def histogramDatagrams(prefix: String, pattern: String, histogram: Histogram, label: String): Seq[Array[Byte]] = {
    val statistics = Option(histogram.getStatistics)

    val statsMap = Seq(s"count" -> histogram.getCount.toString) ++
      statistics.fold(Seq.empty[(String, String)])(s =>
        Seq(
          "max" -> s.getMax.toString,
          "min" -> s.getMin.toString,
          "mean" -> s.getMean.toString,
          "stddev" -> s.getStdDev.toString,
          "p50" -> s.getQuantile(0.5).toString,
          "p75" -> s.getQuantile(0.75).toString,
          "p95" -> s.getQuantile(0.95).toString,
          "p98" -> s.getQuantile(0.98).toString,
          "p99" -> s.getQuantile(0.99).toString,
          "p999" -> s.getQuantile(0.999).toString
        )
      )

    statsMap.map { case (l, v) => datagram(prefix, pattern, s"$label.$l", v, MetricType.Histogram) }
  }
}

class FlinkDatadogStatsDReporter extends AbstractReporter with Scheduled {

  import ReporterFunctions._

  implicit class MetricConfigOps(val c: MetricConfig) {
    def getOptionString(key: String) = Option(c.getString(key, null))
    def getOptionInt(key: String) = Option(c.getInteger(key, Integer.MIN_VALUE)).filter(_ != Integer.MIN_VALUE)
  }

  private var closed: Boolean = false
  private var address: InetSocketAddress = _
  private var socket: DatagramSocket = _
  private var pattern: String = _
  private var prefix: String = _

  val HostConfig = "host"
  val PortConfig = "port"
  val PatternConfig = "pattern"
  val PrefixConfig = "prefix"

  val DefaultHost = "localhost"
  val DefaultPort = 8125
  val DefaultPattern = "key"
  val DefaultPrefix = "flink"

  val LOG = LoggerFactory.getLogger(getClass)

  override def filterCharacters(input: String): String = input.replaceAll(":", "-")

  override def close(): Unit = {
    closed = true
    if (socket != null && !socket.isClosed()) socket.close()
  }

  override def open(config: MetricConfig): Unit = {
    val host = config.getOptionString(HostConfig).getOrElse(DefaultHost)
    val port = config.getOptionInt(PortConfig).getOrElse(DefaultPort)

    address = new InetSocketAddress(host, port)

    LOG.info(s"Starting FlinkDatadogStatsDReporter to send metric reports to $address")

    try {
      socket = new DatagramSocket(0)
      pattern = config.getOptionString(PatternConfig).getOrElse(DefaultPattern)
      prefix = config.getOptionString(PrefixConfig).getOrElse(DefaultPrefix)
    } catch {
      case e: SocketException => throw new RuntimeException("Cannot create datagram socket for StatsD", e)
    }
  }

  protected def socketSend(datagram: Array[Byte]): Unit = try {
    socket.send(new DatagramPacket(datagram, datagram.length, address))
  } catch {
    case ex: IOException =>
      LOG.warn(s"Cannot send metrics to datagram socket at $address", ex)
  }

  override def report(): Unit = try {
    gauges.foreach { case (g, l) => gaugeDatagram(prefix, pattern, g, l).foreach(socketSend) }
    counters.foreach { case (c, l) => socketSend(counterDatagram(prefix, pattern, c, l)) }
    histograms.flatMap(Option(_)).foreach { case (h, l) => histogramDatagrams(prefix, pattern, h, l).foreach(socketSend) }
  } catch {
    case _: ConcurrentModificationException | _: NoSuchElementException =>
      // Do nothing, will send at the next tick
  }
}
