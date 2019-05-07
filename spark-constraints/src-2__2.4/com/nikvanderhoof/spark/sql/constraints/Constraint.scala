package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.{Dataset, DataFrame}

trait Constraint[T] { constraint =>
  def dataset: Dataset[T]
  def violations: DataFrame
  def isViolated: Boolean = !constraint.holds
  def holds: Boolean = violations.isEmpty
}
