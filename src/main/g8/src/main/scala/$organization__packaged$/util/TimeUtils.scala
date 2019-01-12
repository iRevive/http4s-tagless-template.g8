package $organization$.util

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

object TimeUtils {

  val DefaultZone: ZoneOffset = ZoneOffset.UTC

  val DefaultDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DefaultZone)

  def zonedDateTimeNow(): ZonedDateTime = ZonedDateTime.now(DefaultZone)

}
