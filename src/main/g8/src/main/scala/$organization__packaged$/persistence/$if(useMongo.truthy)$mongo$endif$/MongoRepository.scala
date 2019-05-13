package $organization$.persistence.mongo

import cats.data.NonEmptyList
import cats.effect.{Concurrent, ContextShift, IO}
import cats.instances.all.{catsStdInstancesForEither, catsStdInstancesForList, catsStdInstancesForOption}
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import cats.syntax.traverse.toTraverseOps
import $organization$.persistence.mongo.MongoError.UnhandledMongoError
import $organization$.util.Position
import $organization$.util.error.ErrorRaise
import $organization$.util.json.JsonOps
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
import scala.util.control.NonFatal

class MongoRepository[F[_]: Concurrent: ContextShift: ErrorRaise](database: MongoDatabase, collectionName: NonEmptyString) {

  protected[mongo] val collection: F[MongoCollection[BsonDocument]] = {
    val collectionEval = cats.Eval.later(database.getCollection[BsonDocument](collectionName.value))

    IO.eval(collectionEval)
      .to[F]
      .recoverWith { case NonFatal(e) => ErrorRaise[F].raise(UnhandledMongoError(e)) }
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
      result <- doc.traverse(v => JsonOps.decode[A](v.toJson)).pureOrRaise
    } yield result
  }

  final def aggregate[A: Decoder: ClassTag](pipeline: NonEmptyList[Bson]): F[List[A]] =
    for {
      c      <- collection
      docs   <- deferFuture(c.aggregate[BsonDocument](pipeline.toList).toFuture())
      result <- docs.toList.traverse(v => JsonOps.decode[A](v.toJson)).pureOrRaise
    } yield result

  final def findOne[A: Decoder: ClassTag](query: Bson): F[Option[A]] =
    for {
      c      <- collection
      doc    <- deferFuture(c.find[BsonDocument](query).headOption())
      result <- doc.traverse(v => JsonOps.decode[A](v.toJson)).pureOrRaise
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
      result <- docs.toList.traverse(v => JsonOps.decode[A](v.toJson)).pureOrRaise
    } yield result
  }

  final def insertOne[A: Encoder](value: A): F[Unit] =
    for {
      c <- collection
      _ <- deferFuture(c.insertOne(BsonDocument(value.asJson.noSpaces)).toFutureOption())
    } yield ()

  private def deferFuture[R](action: => Future[R])(implicit p: Position): F[R] = {
    IO.fromFuture(IO(action))
      .to[F]
      .flatMap(a => implicitly[ContextShift[F]].shift.map(_ => a))
      .recoverWith { case NonFatal(e) => implicitly[ErrorRaise[F]].raise(UnhandledMongoError(e)) }
  }

}
