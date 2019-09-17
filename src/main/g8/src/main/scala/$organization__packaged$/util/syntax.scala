package $organization$.util

import $organization$.util.config.ToConfigOps
import $organization$.util.error.ToErrorRaiseOps
import $organization$.util.execution.ToRetryOps
import $organization$.util.json.ToJsonOps
import $organization$.util.logging.Loggable.InterpolatorOps
import $organization$.util.syntax.mtl.ToAllMtlOps

object syntax {

  object all     extends ToConfigOps with ToJsonOps with InterpolatorOps with ToRetryOps with ToAllMtlOps
  object config  extends ToConfigOps
  object json    extends ToJsonOps
  object logging extends InterpolatorOps
  object retry   extends ToRetryOps

  object mtl {
    private[syntax] trait ToAllMtlOps extends ToErrorRaiseOps

    object all   extends ToAllMtlOps
    object raise extends ToErrorRaiseOps
  }

}
