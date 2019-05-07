package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.{Column, Dataset, DataFrame}
import org.apache.spark.sql.functions.{coalesce, col, lit}

case class Check[T](dataset: Dataset[T], condition: Column)
    extends Constraint[T] {
  val violations: DataFrame = dataset.where(!condition).toDF
}

case class NotNull[T](dataset: Dataset[T], columns: Seq[Column])
    extends Constraint[T] {
  val violations: DataFrame = dataset.where(coalesce(columns: _*).isNull).toDF
}

case class Unique[T](dataset: Dataset[T], columns: Seq[Column])
    extends Constraint[T] {
  val violations: DataFrame =
    dataset.groupBy(columns: _*).count.where(col("count") =!= 1)
}

case class PrimaryKey[T](dataset: Dataset[T], columns: Seq[Column])
    extends Constraint[T] {
  val violations: DataFrame =
    dataset
      .groupBy(columns: _*)
      .count
      .where(col("count") =!= 1 || coalesce(columns: _*).isNull)
}

case class ForeignKey[T, U](dataset: Dataset[T],
                            columns: Seq[Column],
                            refDataset: Dataset[U],
                            refColumns: Seq[Column])
    extends Constraint[T] {
  val violations: DataFrame = {
    val joinCondition = columns
      .zip(refColumns)
      .map { case (a, b) => a === b }
      .fold(lit(true)) { case (acc, next) => acc && next }
    dataset.join(refDataset, joinCondition, "leftanti")
  }
}
