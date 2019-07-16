package $organization$.persistence
package mongo

import cats.data.NonEmptyList
import cats.effect.{Concurrent, ContextShift, IO}
import cats.effect.syntax.bracket._
import cats.instances.list._
import cats.instances.option._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import $organization$.util.Position
import $organization$.util.error.ErrorRaise
import $organization$.util.syntax.json._
import $organization$.util.syntax.mtl.raise._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{FindOneAndReplaceOptions, ReturnDocument}
import org.mongodb.scala.{MongoCollection, MongoDatabase}

import scala.concurrent.Future
import scala.reflect.ClassTag

class MongoRepository[F[_]: Concurrent: ContextShift: ErrorRaise](database: MongoDatabase, collectionName: NonEmptyString) {

  protected[mongo] val collection: F[MongoCollection[BsonDocument]] = {
    val collectionEval = cats.Eval.later(database.getCollection[BsonDocument](collectionName.value))

    IO.eval(collectionEval)
      .to[F]
      .handleErrorWith(e => MongoError.executionError(e).asLeft.pureOrRaise)
  }

  final def findAndReplaceOne[A: Decoder: Encoder: ClassTag](
      criteria: Bson,
      value: A,
      returnDocument: ReturnDocument,
      upsert: Boolean
  ): F[Option[A]] = {
    def options     = FindOneAndReplaceOptions().upsert(upsert).returnDocument(returnDocument)
    def replacement = BsonDocument(value.asJson.noSpaces)

    for {
      c      <- collection
      doc    <- deferFuture(c.findOneAndReplace(filter = criteria, replacement = replacement, options = options).headOption())
      result <- doc.traverse(decode[A])
    } yield result
  }

  final def aggregate[A: Decoder: ClassTag](pipeline: NonEmptyList[Bson]): F[List[A]] =
    for {
      c      <- collection
      docs   <- deferFuture(c.aggregate[BsonDocument](pipeline.toList).toFuture())
      result <- docs.toList.traverse(decode[A])
    } yield result

  final def findOne[A: Decoder: ClassTag](query: Bson): F[Option[A]] =
    for {
      c      <- collection
      doc    <- deferFuture(c.find[BsonDocument](query).headOption())
      result <- doc.traverse(decode[A])
    } yield result

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  final def find[A: Decoder: ClassTag](query: Bson, pagination: Pagination): F[List[A]] =
    find(query, Some(pagination))

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  final def find[A: Decoder: ClassTag](query: Bson, pagination: Option[Pagination]): F[List[A]] = {
    import eu.timepit.refined.auto.autoUnwrap

    def search(c: MongoCollection[BsonDocument]): Future[Seq[BsonDocument]] = {
      val cursor = c.find[BsonDocument](query)
      pagination.fold(cursor)(p => cursor.skip(p.skip).limit(p.limit)).toFuture()
    }

    for {
      c      <- collection
      docs   <- deferFuture(search(c))
      result <- docs.toList.traverse(decode[A])
    } yield result
  }

  final def insertOne[A: Encoder](value: A): F[Unit] =
    for {
      c <- collection
      _ <- deferFuture(c.insertOne(BsonDocument(value.asJson.noSpaces)).toFutureOption())
    } yield ()

  private def decode[A: Decoder: ClassTag](doc: BsonDocument): F[A] =
    for {
      json     <- io.circe.parser.parse(doc.toJson).leftMap(e => MongoError.executionError(e)).pureOrRaise
      response <- json.decodeF[F, A]
    } yield response

  private def deferFuture[R](action: => Future[R])(implicit p: Position): F[R] =
    IO.fromFuture(IO(action))
      .to[F]
      .handleErrorWith(e => MongoError.executionError(e).asLeft[R].pureOrRaise)
      .guarantee(ContextShift[F].shift)

}
