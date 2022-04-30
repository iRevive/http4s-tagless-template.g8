package $organization$.persistence

import $organization$.util.instances.render.*
import eu.timepit.refined.auto.*
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import io.odin.extras.derivation.render.derived
import io.odin.meta.Render

enum Pagination derives Render {
  case Skip(skip: NonNegInt)
  case Limit(limit: PosInt)
  case SkipLimit(skip: NonNegInt, limit: PosInt)
}
