package $organization$.util

import $organization$.util.config.ToConfigOps

object syntax {

  object all       extends ToConfigOps with ToMapKOps
  object config    extends ToConfigOps
  object mapK      extends ToMapKOps

}
