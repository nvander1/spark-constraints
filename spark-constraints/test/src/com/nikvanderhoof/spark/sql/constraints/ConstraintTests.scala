package com.nikvanderhoof.spark.sql.constraints

import com.github.mrpowers.spark.fast.tests.DataFrameComparer
import com.github.mrpowers.spark.daria.sql.SparkSessionExt._
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions.{col, lit, rand, when}
import org.apache.spark.sql.types.{IntegerType, LongType}
import utest._

object ConstraintTests extends TestSuite with UtestSparkSession with DataFrameComparer {

  import spark.implicits._

  def assertConstraintViolated(constraint: Constraint[_], violations: Dataset[_]) = {
    assert(constraint.isViolated)
    assertSmallDataFrameEquality(constraint.violations, violations.toDF)
  }

  val tests = Tests {
    "check(true) should not flag any rows as violations" - {
      def testCheckTrue(ds: Dataset[_]) = {
        val constraint = Check(ds, lit(true))
        assert(constraint.holds)
      }
      * - testCheckTrue(spark.range(0, 0))
      * - testCheckTrue(spark.range(0, 1000))
      * - testCheckTrue(spark.range(0, 1000000))
      * - testCheckTrue(spark.range(0, 1000000).select(col("id"), rand, rand))
    }

    "check(false) should flag every row as a violation" - {
      def testCheckFalse(ds: Dataset[_]) = {
        val constraint = Check(ds, lit(false))
        assertConstraintViolated(constraint, ds)
      }
      * - testCheckFalse(spark.range(0, 1000))
      * - testCheckFalse(spark.range(0, 1000000))
      * - testCheckFalse(spark.range(0, 1000000).select(col("id"), rand, rand))
    }

    "check(<EXPR>) should correctly identify violations" - {
      "when there are none" - {
        val ds = spark.range(0, 1000)
        val constraint = Check(ds, col("id") >= 0)
        assert(constraint.holds)
      }
      "when they exist" - {
        val ds = spark.range(0, 1000).select((col("id") * -1) as "id")
        val constraint = Check(ds, col("id") > 0)
        assertConstraintViolated(constraint, ds)
      }
    }

    "notNull should correctly identify violations" - {
      "when there are none" - {
        val ds = spark.range(0, 1000)
        val constraint = NotNull(ds, Seq(col("id")))
        assert(constraint.holds)
      }
      "when they exist" - {
        val ds = spark.range(0, 1000).withColumn("my_col", lit(null))
        val constraint = NotNull(ds, Seq(col("my_col")))
        assertConstraintViolated(constraint, ds)
      }
    }

    "unique should correctly identify violations" - {
      "when there are none" - {
        val ds = spark.range(0, 1000)
        val constraint = Unique(ds, Seq(col("id")))
        assert(constraint.holds)
      }
      "when specified over a single column" - {
        val ds = spark.range(0, 5).withColumn("bad_id", lit(2))
        val constraint = Unique(ds, Seq(col("bad_id")))
        val expectedViolations = Seq((2, 5L)).toDF("bad_id", "count")
        assertConstraintViolated(constraint, expectedViolations)
      }
      "when specified over multiple columns" - {
        val ds = spark.range(0, 5)
          .withColumn("bad_2", lit(2))
          .withColumn("bad_3", lit(3))
        val constraint = Unique(ds, Seq(col("bad_2"), col("bad_3")))
        val expectedViolations = Seq((2, 3, 5L)).toDF("bad_2", "bad_3", "count")
        assertConstraintViolated(constraint, expectedViolations)
      }
    }

    "primary key should correctly identify violations" - {
      "when there are none" - {
        val ds = spark.range(0, 1000)
        val constraint = PrimaryKey(ds, Seq(col("id")))
        assert(constraint.holds)
      }
      "when the key is null" - {
        val ds = spark.range(0, 5).withColumn("bad_id", lit(null))
        val constraint = PrimaryKey(ds, Seq(col("bad_id")))
        val expectedViolations = Seq((null, 5L)).toDF("bad_id", "count")
        assertConstraintViolated(constraint, expectedViolations)
      }
      "when the key is not unique" - {
        * - {
        val ds = spark.range(0, 5).withColumn("bad_id", lit(2))
        val constraint = PrimaryKey(ds, Seq(col("bad_id")))
        val expectedViolations = Seq((2, 5L)).toDF("bad_id", "count")
        assertConstraintViolated(constraint, expectedViolations)
        }
        * - {
          val ds = spark.range(0, 5)
            .withColumn("bad_2", lit(2))
            .withColumn("bad_3", lit(3))
          val constraint = PrimaryKey(ds, Seq(col("bad_2"), col("bad_3")))
          val expectedViolations = Seq((2, 3, 5L)).toDF("bad_2", "bad_3", "count")
          assertConstraintViolated(constraint, expectedViolations)
        }
      }

      "when they key is unique, but some entries are null" - {
        val ds = spark.createDF(
          List((1), (null), (3)),
          List(("my_key", IntegerType, true))
        )
        val constraint = PrimaryKey(ds, List(col("my_key")))
        val expectedViolations = spark.createDF(
          List((null, 1L)),
          List(("my_key", IntegerType, true), ("count", LongType, false))
        )
        assertConstraintViolated(constraint, expectedViolations)
      }
    }

    "foreign key should correctly identify violations" - {
      "when there are none" - {
        val a = spark.range(0, 100)
        val b = spark.range(0, 100)
        val constraint = ForeignKey(a, Seq(a("id")), b, Seq(b("id")))
        assert(constraint.holds)
      }
      "when the key is not present in the referenced table" - {
        val a = spark.range(0, 100)
        val b = spark.range(20, 100)
        val constraint = ForeignKey(a, Seq(a("id")), b, Seq(b("id")))
        val expectedViolations = a.where("id < 20")
        assertConstraintViolated(constraint, expectedViolations)
      }
    }

  }

}
