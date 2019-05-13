package $organization$.persistence.mongo

import $organization$.util.logging.Loggable
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}

@scalaz.deriving(Loggable)
final case class Pagination(skip: NonNegInt, limit: PosInt)

object Pagination {

  def limit(value: PosInt): Pagination = Pagination(0, value)

}
