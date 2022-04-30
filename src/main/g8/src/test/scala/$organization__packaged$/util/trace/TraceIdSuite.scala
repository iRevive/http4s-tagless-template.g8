package $organization$.util.trace

import java.util.UUID

import $organization$.test.SimpleEffSuite
import $organization$.util.trace.TraceId./

object TraceIdSuite extends SimpleEffSuite {

  test("generate a value from random uuid") {
    for {
      traceId <- TraceId.randomUuid
    } yield expect(traceId.value.isInstanceOf[UUID])
  }

  test("generate a correct sub id") {
    forall { (string: String) =>
      for {
        traceId <- TraceId.randomUuid
      } yield {
        val withChild = traceId.child(TraceId.Const(string))

        withChild match {
          case TraceId.Uuid(_) / TraceId.Const(value) => expect(value == string)
          case other                                  => failure(s"The structure is incorrect \$traceId")
        }
      }
    }
  }

}
