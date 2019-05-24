package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.SparkSession
import org.apache.log4j.{Logger, Level}
import utest._

trait UtestSparkSession { self: TestSuite =>
  Logger.getLogger("org").setLevel(Level.OFF)
  val spark = SparkSession
    .builder()
    .master("local[*]")
    .appName("Tests")
    .config("spark.sql.shuffle.partitions", "1")
    .getOrCreate()
  val sc = spark.sparkContext
  val sql = spark.sqlContext
  override def utestAfterAll(): Unit = {
    spark.close()
  }
}
