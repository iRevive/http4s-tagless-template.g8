package $organization$.util

import $organization$.util.json.JsonCodecs
import $organization$.util.logging.RenderInstances

object instances {

  object all    extends JsonCodecs with RenderInstances
  object circe  extends JsonCodecs
  object render extends RenderInstances

}
