package $organization$.util

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import $organization$.test.BaseSpec

class TimeUtilsSpec extends BaseSpec {

  "TimeUtils" should {

    "use UTC timezone" in {
      TimeUtils.DefaultZone shouldBe ZoneOffset.UTC
    }

    "use UTC timezone in the default date time formatter" in {
      val expectedFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)

      TimeUtils.DefaultDateTimeFormat.toString shouldBe expectedFormat.toString
    }

    "use UTC timezone in #zonedDateTimeNow" in {
      TimeUtils.zonedDateTimeNow().getZone.getId shouldBe ZoneOffset.UTC.getId
    }

  }

}
