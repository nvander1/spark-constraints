package com.nikvanderhoof.spark.sql

import org.apache.spark.sql.{Column, Dataset}
import org.apache.spark.sql.constraints.datasetToNamedDataset

package object constraints {
  implicit def dataset2NamedDataset[T](dataset: Dataset[T]) = datasetToNamedDataset(dataset)
  implicit def dataset2Constrained[T](dataset: Dataset[T]) = ConstrainedDataset(dataset)

  private[constraints] def columnsToStringRep(columns: Seq[Column]) = {
    if (columns.size > 1) {
      columns.mkString("(", ", ", ")")
    } else {
      columns.mkString(",")
    }
  }
}
