package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.DataFrame

trait Constraint[T] { constraint =>
  def data: NamedDataset[T]
  def violations: DataFrame
  def isViolated: Boolean = !constraint.holds
  def holds: Boolean = violations.take(1).isEmpty
}
