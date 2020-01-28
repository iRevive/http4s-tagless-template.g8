package $organization$.util

import $organization$.util.config.ToConfigOps
import $organization$.util.error.ToErrorRaiseOps
import $organization$.util.json.ToJsonOps

object syntax {

  object all    extends ToConfigOps with ToJsonOps with mtl.ToAllMtlOps
  object config extends ToConfigOps
  object json   extends ToJsonOps

  object mtl {
    private[syntax] trait ToAllMtlOps extends ToErrorRaiseOps

    object all   extends ToAllMtlOps
    object raise extends ToErrorRaiseOps
  }

}
