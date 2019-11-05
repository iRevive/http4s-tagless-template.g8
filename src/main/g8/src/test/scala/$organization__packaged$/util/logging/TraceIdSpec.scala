package $organization$.util.logging

import java.util.UUID

import $organization$.test.BaseSpec
import $organization$.util.logging.TraceId./

class TraceIdSpec extends BaseSpec {

  "TraceId" should {

    "generate a value from random uuid" in EffectAssertion() {
      for {
        traceId <- TraceId.randomUuid
      } yield traceId.value shouldBe a[UUID]
    }

    "generate a correct sub id" in forAll { string: String =>
      EffectAssertion() {
        for {
          traceId <- TraceId.randomUuid
        } yield {
          inside(traceId.child(TraceId.Text(string))) {
            case TraceId.Uuid(_) / TraceId.Text(value) =>
              value shouldBe string
          }
        }
      }
    }

  }

}
