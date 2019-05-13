package $organization$.api

import org.http4s.HttpApp

final case class ApiModule[F[_]](routes: HttpApp[F], config: ApiConfig)
