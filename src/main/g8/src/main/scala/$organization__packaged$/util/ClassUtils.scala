package $organization$.util

import scala.reflect.ClassTag

object ClassUtils {

  /**
    * Hack to avoid scala compiler bug with getSimpleName
    * @see https://issues.scala-lang.org/browse/SI-2034
    */
  def getClassSimpleName(clazz: Class[_]): String = {
    try {
      clazz.getSimpleName.stripSuffix("\$")
    } catch {
      case _: InternalError =>
        val fullName      = clazz.getName.stripSuffix("\$")
        val fullClassName = fullName.substring(fullName.lastIndexOf(".") + 1)
        fullClassName.substring(fullClassName.lastIndexOf("\$") + 1)
    }
  }

  def classSimpleName[A](implicit ct: ClassTag[A]): String = {
    getClassSimpleName(ct.runtimeClass)
  }

}
