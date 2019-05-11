package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{expr, col}

import utest._

object SyntaxTests extends TestSuite with UtestSparkSession {

  import spark.implicits._

  val peopleDF = Seq(
    (1, "Alice", 25),
    (2, "Bob", 20),
    (3, "Carol", 26)
  ).toDF("id", "name", "age")

  val booksDF = Seq(
    (1, "Introduction to Programming"),
    (2, "Number Systems"),
    (3, "Partial Differential Equations")
  ).toDF("id", "title")

  val bookAuthorsDF = Seq(
    (1, 1),
    (2, 2),
    (3, 3)
  ).toDF("people_id", "book_id")

  val tests = Tests {
    "constrain syntax" - {
      constrain(peopleDF) {
        primaryKey('id)
        check(col("age") >= 0 && col("age") < 120)
        notNull(col("name"))
        notNull('age)
      } match {
        case ConstrainedDataset(name, data, violations) =>
          assert(name == "peopleDF")
          assert(violations.size == 0)
      }

      constrain(booksDF) {
        primaryKey('id)
        notNull('title)
        unique('title)
      } match {
        case ConstrainedDataset(name, data, violations) =>
          assert(name == "booksDF")
          assert(violations.size == 0)
      }

      constrain(bookAuthorsDF) {
        primaryKey('people_id, 'book_id)
        foreignKey('people_id) references peopleDF at 'id
        foreignKey('people_id) references booksDF at 'id
      } match {
        case ConstrainedDataset(name, data, violations) =>
          assert(name == "bookAuthorsDF")
          assert(violations.size == 0)
      }
    }
  }
}
