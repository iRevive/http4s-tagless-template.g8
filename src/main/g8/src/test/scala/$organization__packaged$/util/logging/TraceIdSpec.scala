package $organization$.util.logging

import java.util.UUID

import $organization$.test.BaseSpec

class TraceIdSpec extends BaseSpec {

  "TraceId" should {

    "generate a value from random uuid" in EffectAssertion() {
      for {
        traceId <- TraceId.randomUuid
      } yield noException shouldBe thrownBy(UUID.fromString(traceId.value))
    }

    "generate a correct sub id" in forAll { (string: String, int: Int) =>
      EffectAssertion() {
        for {
          traceId <- TraceId.randomUuid
        } yield {
          traceId.subId(string).value shouldBe traceId.value + "#" + string
          traceId.subId(int).value shouldBe traceId.value + "#" + int
        }
      }
    }

  }

}
