package $organization$.persistence

import $organization$.util.instances.render._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import io.odin.meta.Render

@scalaz.annotation.deriving(Render)
sealed trait Pagination

object Pagination {

  final case object NoPagination                             extends Pagination
  final case class Skip(skip: NonNegInt)                     extends Pagination
  final case class Limit(limit: PosInt)                      extends Pagination
  final case class SkipLimit(skip: NonNegInt, limit: PosInt) extends Pagination

}
