package $organization$.util.error

import cats.Applicative
import cats.effect.Sync

sealed trait ErrorIdGen[F[_]] {
  def gen: F[String]
}

object ErrorIdGen {

  def apply[F[_]](implicit ev: ErrorIdGen[F]): ErrorIdGen[F] = ev

  def alphanumeric[F[_]: Sync](length: Int): ErrorIdGen[F] =
    new ErrorIdGen[F] {
      override def gen: F[String] = Sync[F].delay(scala.util.Random.alphanumeric.take(length).mkString)
    }

  def const[F[_]: Applicative](value: String): ErrorIdGen[F] =
    new ErrorIdGen[F] {
      override def gen: F[String] = Applicative[F].pure(value)
    }

}
