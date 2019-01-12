package $organization$.persistence.mongo

import $organization$.util.logging.{Loggable, LoggableDerivation}
import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}

final case class Pagination(skip: NonNegInt, limit: PosInt)

object Pagination {

  def limit(value: PosInt): Pagination = Pagination(0, value)

  implicit val loggableInstance: Loggable[Pagination] = LoggableDerivation.derive

}
