package $organization$.persistence

$if(useMongo.truthy)$
import org.mongodb.scala.MongoDatabase
$endif$

$if(useMongo.truthy)$
final case class PersistenceModule(mongoDatabase: MongoDatabase)
$else$
final case class PersistenceModule()
$endif$