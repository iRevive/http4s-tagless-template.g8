package $organization$.util

import sourcecode.Enclosing
import io.odin.meta.Render

final case class Position(enclosing: sourcecode.Enclosing, line: sourcecode.Line) {
  def fullPosition: String = s"\${enclosing.value}:\${line.value}"
}

object Position {

  implicit def generate(implicit enclosing: sourcecode.Enclosing, line: sourcecode.Line): Position =
    new Position(removeAnonEnclosing(enclosing), line)

  private def removeAnonEnclosing(enclosing: Enclosing): String =
    enclosing.value
      .replace("#applyOrElse", "")
      .replace("applyOrElse", "")
      .trim

  implicit val renderPosition: Render[Position] = _.fullPosition

}
