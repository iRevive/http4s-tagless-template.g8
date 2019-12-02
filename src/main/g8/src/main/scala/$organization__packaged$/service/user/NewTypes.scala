package $organization$.service.user

import eu.timepit.refined.types.numeric.PosInt
import doobie.refined.implicits._
import doobie.util.{Get, Put}
import $organization$.util.logging.Loggable
import io.estatico.newtype.macros.newtype

// \$COVERAGE-OFF\$
@SuppressWarnings(Array("org.wartremover.warts.ExplicitImplicitTypes"))
object NewTypes {

  @newtype
  @scalaz.annotation.deriving(Loggable)
  final case class UserId(toInt: PosInt)

  object UserId {
    implicit val userIdGet: Get[UserId] = Get[PosInt].map(UserId.apply)
    implicit val userIdPut: Put[UserId] = Put[PosInt].contramap(_.toInt)
  }

}
// \$COVERAGE-ON\$
