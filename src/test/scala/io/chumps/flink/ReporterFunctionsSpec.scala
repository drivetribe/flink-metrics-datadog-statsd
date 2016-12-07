package io.chumps.flink

import org.apache.flink.metrics._
import org.scalatest.{FlatSpec, Matchers}
import ReporterFunctions._

class ReporterFunctionsSpec extends FlatSpec with Matchers {

  "The `extract` function" should "produce name and tags given a perfecty matching pattern and a label" in {
    extract("foo.bar.baz", "some_foo.some_bar.some_baz.count") should equal(
      ("count", Map("foo" -> "some_foo", "bar" -> "some_bar", "baz" -> "some_baz"))
    )
  }

  it should "assign a default name when the pattern consumes the whole label" in {
    extract("foo.bar.baz", "some_foo.some_bar.some_baz") should equal(
      (DefaultMetricName, Map("foo" -> "some_foo", "bar" -> "some_bar", "baz" -> "some_baz"))
    )
  }

  it should "support a pattern name that is longer than the label" in {
    extract("foo.bar.baz.fox.cat", "some_foo.some_bar.some_baz") should equal(
      (DefaultMetricName, Map("foo" -> "some_foo", "bar" -> "some_bar", "baz" -> "some_baz"))
    )
  }

  it should "support a pattern name that is shorter than the label and derive the metric name" in {
    extract("foo.bar.baz", "some_foo.some_bar.some_baz.some_subdomain.some_key") should equal(
      ("some_subdomain.some_key", Map("foo" -> "some_foo", "bar" -> "some_bar", "baz" -> "some_baz"))
    )
  }

  it should "detect IP addresses and replace `.` with `-` before gathering the tags" in {
    extract("host.bar.baz", "123.132.231.123.some_bar.some_baz.count") should equal(
      ("count", Map("host" -> "ip-123-132-231-123", "bar" -> "some_bar", "baz" -> "some_baz"))
    )
  }

  "The `gaugeDatagram` function" should "not produce a datagram if the provided `Gauge` has a `null` value" in {
    val g = new Gauge[String] { def getValue: String = null }

    gaugeDatagram("test", "foo.bar.baz", g, "some.scoped.key.gauge_value") shouldBe empty
  }

  it should "produce a datagram if the provided `Gauge` has a non-null value" in {
    val g = new Gauge[Int] { def getValue: Int = 123 }

    gaugeDatagram("test", "foo.bar.baz", g, "some.scoped.key.gauge_value").map(new String(_)) should equal(
      Some("test.gauge_value:123|g|#foo:some,#bar:scoped,#baz:key")
    )
  }

  "The `counterDatagram` function "should "produce a datagram with a `Counter` value" in {
    val c = new SimpleCounter()
    c.inc(100)

    new String(counterDatagram("test", "foo.bar.baz", c, "some.scoped.key.counter_value")) should equal(
      "test.counter_value:100|c|#foo:some,#bar:scoped,#baz:key"
    )
  }

  "The `histogramDatagrams` function "should "produce a set of datagrams with a `Histogram` value" in {
    val h = new Histogram() {
      def getCount() = 1234
      def getStatistics() = new HistogramStatistics() {
        def getMax(): Long = 10000
        def getMean(): Double = 5000
        def getMin(): Long = 0
        def getQuantile(x: Double): Double = x * 100
        def getStdDev(): Double = 0.123
        def getValues(): Array[Long] = fail("Unexpected call to `getValues`")
        def size(): Int = fail("Unexpected call to `size`")
      }
      def update(x: Long) = fail("Unexpected call to `update`")
    }

    val datagrams = histogramDatagrams(
      "test", "foo.bar.baz", h, "some.scoped.key.histogram_value"
    ).map(new String(_))

    datagrams should contain theSameElementsAs(
      Seq(
        "test.histogram_value.count:1234|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.max:10000|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.min:0|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.mean:5000.0|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.stddev:0.123|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.p50:50.0|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.p75:75.0|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.p95:95.0|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.p98:98.0|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.p99:99.0|h|#foo:some,#bar:scoped,#baz:key",
        "test.histogram_value.p999:99.9|h|#foo:some,#bar:scoped,#baz:key"
      )
    )
  }
}
