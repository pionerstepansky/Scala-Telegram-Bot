package Parsers

object IdGenerator {
  private val IdGenerator = Stream.from(1).iterator

  def GetId = IdGenerator.next()
}
