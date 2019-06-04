package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.{Column, Dataset, DataFrame}
import org.apache.spark.sql.functions.{coalesce, col, lit}

case class NamedDataset[T](name: String, dataset: Dataset[T])

case class Check[T](data: NamedDataset[T], condition: Column)
extends Constraint[T] {
  val violations: DataFrame = data.dataset.where(!condition).toDF
  override def toString: String = s"Check(${data.name}, $condition)"
}

case class NotNull[T](data: NamedDataset[T], columns: Seq[Column])
extends Constraint[T] {
  val violations: DataFrame =
    data.dataset.where(coalesce(columns: _*).isNull).toDF
  override def toString: String =
    s"NotNull(${data.name}, ${columnsToStringRep(columns)})"
}

case class Unique[T](data: NamedDataset[T], columns: Seq[Column])
    extends Constraint[T] {
  val violations: DataFrame =
    data.dataset.groupBy(columns: _*).count.where(col("count") =!= 1)
  override def toString: String =
    s"Unique(${data.name}, ${columnsToStringRep(columns)})"
}

case class PrimaryKey[T](data: NamedDataset[T], columns: Seq[Column])
extends Constraint[T] {
  val violations: DataFrame =
    data
      .dataset
      .groupBy(columns: _*)
      .count
      .where(col("count") =!= 1 || coalesce(columns: _*).isNull)
  override def toString: String =
    s"PrimaryKey(${data.name}, ${columnsToStringRep(columns)})"
}

case class ForeignKey[T, U](data: NamedDataset[T],
                            columns: Seq[Column],
                            refData: NamedDataset[U],
                            refColumns: Seq[Column])
extends Constraint[T] {
  val violations: DataFrame = {
    val joinCondition = columns
      .zip(refColumns)
      .map { case (a, b) => a === b }
      .fold(lit(true)) { case (acc, next) => acc && next }
    data.dataset.join(refData.dataset, joinCondition, "leftanti")
  }
  override def toString: String = {
    val name = data.name
    val columnsString = columnsToStringRep(columns)
    val refName = refData.name
    val refColumnsString = columnsToStringRep(refColumns)
    s"ForeignKey($name, $columnsString, $refName, $refColumnsString)"
  }
}

/**
 * A dataset with constraints applied.
 */
class ConstrainedDataset[T](data: NamedDataset[T],
                            val constraints: Seq[Constraint[T]]) {
  val name: String = data.name
  val dataset: Dataset[T] = data.dataset
  val violations = constraints.collect {
    case c if c.isViolated => (c.toString, c.violations)
  }
  override def toString: String =
    s"ConstrainedDataset(${data.name}, ${data.dataset}, $constraints)"

  def showViolations: Unit = {
    for ((name, violation) <- violations) {
      println(name)
      violation.show
    }
  }

  def add(f: NamedDataset[T] => Constraint[T]): ConstrainedDataset[T] = {
    ConstrainedDataset(data, constraints :+ f(data))
  }

  def withPrimaryKey(columns: Column *): ConstrainedDataset[T] =
    add(data => PrimaryKey(data, columns))

  def withUnique(columns: Column *): ConstrainedDataset[T] =
    add(data => Unique(data, columns))

  def withNotNull(columns: Column *): ConstrainedDataset[T] =
    add(data => NotNull(data, columns))

  def withCheck(column: Column): ConstrainedDataset[T] =
    add(data => Check(data, column))

  def withForeignKey(columns: Column *) = new {
    def to[U](refData: Dataset[U]) = new {
      def at(refColumns: Column *) =
        add(data => ForeignKey(data, columns, refData, refColumns))
    }
  }
}

object ConstrainedDataset {
  def apply[T](data: NamedDataset[T], constraints: Seq[Constraint[T]])
  : ConstrainedDataset[T] = new ConstrainedDataset(data, constraints)

  def apply[T](data: Dataset[T], constraints: Seq[Constraint[T]])
  : ConstrainedDataset[T] = apply(dataset2NamedDataset(data), constraints)

  def apply[T](data: Dataset[T]): ConstrainedDataset[T] =
    apply(data, Nil)

  def unapply[T](constrainedData: ConstrainedDataset[T])
  : Option[(String, Dataset[T], Seq[(String, DataFrame)])] = {
    import constrainedData._
    Some((name, dataset, violations))
  }
}
