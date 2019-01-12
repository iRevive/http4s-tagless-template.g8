package $organization$.api

import org.http4s.HttpApp

final case class ApiModule[G[_]](routes: HttpApp[G], config: ApiConfig)
