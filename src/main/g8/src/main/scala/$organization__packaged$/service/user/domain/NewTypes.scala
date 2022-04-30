package $organization$.service.user.domain

import cats.{Contravariant, Show}
import $organization$.persistence.postgres.Persisted
import $organization$.util.instances.render.*
import doobie.refined.implicits.*
import doobie.util.{Get, Put}
import eu.timepit.refined.api.{RefType, Refined}
import eu.timepit.refined.collection.{NonEmpty, Size}
import eu.timepit.refined.numeric.{Interval, Positive}
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyFiniteString
import io.circe.refined.*
import io.circe.{Codec, Decoder, Encoder}
import io.odin.meta.Render
import monix.newtypes.*

type PersistedUser = Persisted[UserId, User]

type UserId = UserId.Type
object UserId extends RefinedNewtype[PosInt]

type Username = Username.Type
object Username extends RefinedNewtype[NonEmptyFiniteString[50]]

type Password = Password.Type
object Password extends RefinedNewtype[NonEmptyFiniteString[50]]

abstract class RefinedNewtype[A](using Render[A], Show[A], Get[A], Put[A], Decoder[A], Encoder[A]) extends NewtypeWrapped[A] {
  given Render[Type] = derive[Render]
  given Show[Type]   = derive[Show]
  given Get[Type]    = derive[Get]
  given Put[Type]    = derive[Put]
  given Codec[Type]  = Codec.from(derive[Decoder], derive[Encoder])
}

given refTypeShow[F[_, _], T, P](using rt: RefType[F], gt: Show[T]): Show[F[T, P]] =
  Contravariant[Show].contramap(gt)(rt.unwrap)
