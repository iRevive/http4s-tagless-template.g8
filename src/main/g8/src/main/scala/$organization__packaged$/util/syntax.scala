package $organization$.util

import $organization$.util.config.ToConfigOps
import $organization$.util.error.{ToErrorHandleOps, ToErrorRaiseOps}
import $organization$.util.logging.Loggable.InterpolatorOps
import $organization$.util.syntax.mtl.ToAllMtlOps

object syntax {

  object all     extends ToConfigOps with InterpolatorOps with ToAllMtlOps
  object config  extends ToConfigOps
  object logging extends InterpolatorOps

  object mtl {
    private[syntax] trait ToAllMtlOps extends ToErrorHandleOps with ToErrorRaiseOps

    object all    extends ToAllMtlOps
    object handle extends ToErrorHandleOps
    object raise  extends ToErrorRaiseOps
  }

}
