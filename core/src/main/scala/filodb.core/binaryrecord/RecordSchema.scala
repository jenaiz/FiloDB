package filodb.core.binaryrecord

import scala.language.existentials

import filodb.core.metadata.Column.ColumnType

final case class Field(num: Int, colType: ColumnType, fixedDataOffset: Int, fieldType: FieldType[_]) {
  final def get[T](record: BinaryRecord): T = fieldType.asInstanceOf[FieldType[T]].extract(record, this)
  final def getAny(record: BinaryRecord): Any = fieldType.extract(record, this)
}

class RecordSchema(columnTypes: Seq[ColumnType]) {
  // Computes offsets for every field, where they would go etc
  val numFields = columnTypes.length

  // Number of 32-bit words at beginning for null check
  val nullBitWords = (numFields + 31) / 32
  val fixedDataStartOffset = nullBitWords * 4

  // val fields - fixed data field section
  var curOffset = fixedDataStartOffset
  val fields = columnTypes.zipWithIndex.map { case (colType, no) =>
    val field = Field(no, colType, curOffset, FieldType.columnToField(colType))
    curOffset += field.fieldType.numFixedBytes
    field
  }.toArray

  val variableDataStartOffset = curOffset
}