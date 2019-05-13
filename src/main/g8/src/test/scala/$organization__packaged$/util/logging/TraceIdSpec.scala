package $organization$.util.logging

import java.util.UUID

import $organization$.test.BaseSpec

class TraceIdSpec extends BaseSpec {

  "TraceId" should {

    "generate a value from random uuid" in {
      val traceId = TraceId.randomUuid()

      noException shouldBe thrownBy(UUID.fromString(traceId.value))
    }

    "generate a correct sub id" in {
      val traceId = TraceId.randomUuid()

      val expectedTraceId = traceId.value + "#" + 1

      traceId.subId(1).value shouldBe expectedTraceId
    }

  }

}
