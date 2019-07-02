package $organization$.util

import $organization$.util.config.ToConfigOps
import $organization$.util.error.ToErrorRaiseOps
import $organization$.util.logging.Loggable.InterpolatorOps
import $organization$.util.syntax.mtl.ToAllMtlOps

object syntax {

  object all     extends ToConfigOps with InterpolatorOps with ToRetryOps with ToAllMtlOps
  object config  extends ToConfigOps
  object logging extends InterpolatorOps
  object retry   extends ToRetryOps

  object mtl {
    private[syntax] trait ToAllMtlOps extends ToErrorRaiseOps

    object all   extends ToAllMtlOps
    object raise extends ToErrorRaiseOps
  }

}
