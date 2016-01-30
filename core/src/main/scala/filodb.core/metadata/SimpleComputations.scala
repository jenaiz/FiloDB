package filodb.core.metadata

import org.scalactic._
import org.velvia.filo.RowReader

import filodb.core._
import filodb.core.Types._


object SimpleComputations {
  import ComputedKeyTypes._
  import Column.ColumnType._

  object ConstStringComputation extends ColumnComputation {
    def funcName: String = "string"

    def analyze(expr: String,
                dataset: TableName,
                schema: Seq[Column]): ComputedColumn Or InvalidComputedColumnSpec = {
      for { args <- fixedNumArgs(expr, 1) }
      yield {
        ComputedColumn(0, expr, dataset, StringColumn,
                       new ComputedStringKeyType((x: RowReader) => args.head))
      }
    }
  }

  /**
   * Syntax: :getOrElse <colName> <defaultValue>
   * returns <defaultValue> if <colName> is null
   */
  object GetOrElseComputation extends SingleColumnComputation {
    def funcName: String = "getOrElse"

    def analyze(expr: String,
                dataset: TableName,
                schema: Seq[Column]): ComputedColumn Or InvalidComputedColumnSpec = {
      for { info <- parse(expr, schema)
            defaultValue <- parseParam(info.keyType, info.param) }
      yield { computedColumnWithDefault(expr, dataset, info)(defaultValue)(x => x) }
    }
  }

  /**
   * Syntax: :round <colName> <roundingValue>
   * Valid for: IntColumn, LongColumn, DoubleColumn
   * Rounds the numeric value to the nearest multiple of <roundingValue>.
   * Examples:
   *
   *   :round intCol 10000 where  intCol=12345 -> 10000,  19999 -> 10000
   *   :rount doubleCol 1000.0 where doubleCol=1999.9->1000
   *
   * NOTE: the rounding value is used as the default value if the source column is null.
   */
  object RoundComputation extends SingleColumnComputation {
    import Column.ColumnType._

    def funcName: String = "round"

    def analyze(expr: String,
                dataset: TableName,
                schema: Seq[Column]): ComputedColumn Or InvalidComputedColumnSpec = {
      for { info <- parse(expr, schema, Set(IntColumn, LongColumn, DoubleColumn))
            roundingValue <- parseParam(info.keyType, info.param) }
      yield {
        val func = (info.colType match {
          case IntColumn =>
            val round = roundingValue.asInstanceOf[Int]
            (i: Int) => i / round * round
          case LongColumn =>
            val round = roundingValue.asInstanceOf[Long]
            (i: Long) => i / round * round
          case DoubleColumn =>
            val round = roundingValue.asInstanceOf[Double]
            (i: Double) => Math.floor(i / round) * round
          case o: Column.ColumnType => ???
        }).asInstanceOf[info.keyType.T => info.keyType.T]
        computedColumnWithDefault(expr, dataset, info)(roundingValue)(func)
      }
    }
  }

}