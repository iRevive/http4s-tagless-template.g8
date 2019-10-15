package $organization$.util

import scala.reflect.ClassTag

object ClassUtils {

  /**
    * Hack to avoid scala compiler bug with getSimpleName
    * @see https://issues.scala-lang.org/browse/SI-2034
    */
  def getClassSimpleName(clazz: Class[_]): String = {
    def fallback = {
      val fullName      = clazz.getName.substring(0, clazz.getName.indexOf("\$"))
      val fullClassName = fullName.substring(fullName.lastIndexOf(".") + 1)
      fullClassName.substring(fullClassName.lastIndexOf("\$") + 1)
    }

    try {
      val name = clazz.getSimpleName.stripSuffix("\$")
      if (name.trim.isEmpty) fallback else name
    } catch {
      case _: InternalError =>
        fallback
    }
  }

  def classSimpleName[A](implicit ct: ClassTag[A]): String =
    getClassSimpleName(ct.runtimeClass)

}
