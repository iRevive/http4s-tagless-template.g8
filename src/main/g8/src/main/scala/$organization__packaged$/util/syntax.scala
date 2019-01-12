package $organization$.util

import $organization$.util.config.ConfigSyntax

object syntax {

  object all       extends ConfigSyntax with ToTracedLikeOps with ToResourceOps
  object config    extends ConfigSyntax
  object resources extends ToResourceOps
  object tracedTo  extends ToTracedLikeOps

}
