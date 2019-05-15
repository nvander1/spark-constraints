package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.SparkSession
import utest._

trait UtestSparkSession { self: TestSuite =>
  val spark = SparkSession.builder().master("local[*]").appName("Tests").config("spark.sql.shuffle.partitions", "1").getOrCreate()
  val sc = spark.sparkContext
  val sql = spark.sqlContext
  sc.setLogLevel("ERROR")
  override def utestAfterAll(): Unit = {
    spark.close()
  }
}
