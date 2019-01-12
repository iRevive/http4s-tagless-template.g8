package $organization$.util
package error

import $organization$.util.logging.Loggable

trait BaseError {

  def message: String

  def pos: Position

  def toRuntimeException: RuntimeException = new RuntimeException(toString)

  override def toString: String = Loggable[BaseError].show(this)
}

trait ThrowableError extends BaseError {

  def cause: Throwable

  override def toRuntimeException: RuntimeException = new RuntimeException(toString, cause)

  override def message: String = Loggable[Throwable].show(cause)

}

object ThrowableError {

  def apply(exception: Throwable)(implicit position: Position): ThrowableError =
    new ThrowableError {
      override def pos: Position =
        position

      override def cause: Throwable =
        exception
    }

  def unapply(arg: ThrowableError): Option[Throwable] = {
    Some(arg.cause)
  }

  implicit val throwableErrorLoggable: Loggable[ThrowableError] = Loggable.instance { error =>
    val rawClassName = ClassUtils.getClassSimpleName(error.getClass)
    val className    = if (rawClassName.contains("anon")) "ThrowableError" else rawClassName
    s"\$className(message = \${error.message}, cause = \${error.cause}, pos = \${error.pos.fullPosition})"
  }

}

object BaseError {

  implicit val baseErrorLoggable: Loggable[BaseError] = Loggable.instance {
    case e: ThrowableError =>
      Loggable[ThrowableError].show(e)

    case e =>
      val className = ClassUtils.getClassSimpleName(e.getClass)
      s"\$className(message = \${e.message}, pos = \${e.pos.fullPosition})"
  }

}
