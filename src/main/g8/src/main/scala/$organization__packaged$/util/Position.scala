package $organization$.util

import sourcecode.Enclosing

final case class Position(enclosing: sourcecode.Enclosing, line: sourcecode.Line) {
  def fullPosition: String = s"\${enclosing.value}:\${line.value}"
}

object Position {

  implicit def generate(implicit enclosing: sourcecode.Enclosing, line: sourcecode.Line): Position =
    new Position(removeAnonEnclosing(enclosing), line)

  private def removeAnonEnclosing(enclosing: Enclosing): String =
    enclosing.value
      .replace("\$anonfun", "")
      .replace("#applyOrElse", "")
      .replace("applyOrElse", "")
      .trim

}
