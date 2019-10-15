package $organization$.persistence

import $organization$.util.logging.Loggable
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}

@scalaz.annotation.deriving(Loggable)
sealed trait Pagination

object Pagination {

  final case object NoPagination                             extends Pagination
  final case class Skip(skip: NonNegInt)                     extends Pagination
  final case class Limit(limit: PosInt)                      extends Pagination
  final case class SkipLimit(skip: NonNegInt, limit: PosInt) extends Pagination

}
