package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.Dataset

case class NamedDataset[T](name: String, dataset: Dataset[T])
