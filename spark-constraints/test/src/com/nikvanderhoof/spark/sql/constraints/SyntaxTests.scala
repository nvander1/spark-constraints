package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{expr, col}

import utest._

object SyntaxTests extends TestSuite with UtestSparkSession {

  import spark.implicits._

  val peopleDF = Seq(
    (1, "Alice", 25),
    (2, "Bob", 20),
    (3, "Carol", -1)
  ).toDF("id", "name", "age").as("people")

  val booksDF = Seq(
    (1, "Introduction to Programming"),
    (2, "Number Systems"),
    (3, "Partial Differential Equations")
  ).toDF("id", "title").as("books")

  val bookAuthorsDF = Seq(
    (1, 1),
    (2, 2),
    (3, 4)
  ).toDF("people_id", "book_id").as("book_authors")

  val tests = Tests {
    "constrain syntax" - {
      peopleDF
        .primaryKey('id)
        .check(col("age") >= 0 && col("age") < 120)
        .notNull(col("name"))
        .notNull('age)
      match {
        case cd @ ConstrainedDataset(name, data, violations) =>
          assert(name == "people")
          assert(violations.size == 1)
          cd.showViolations
      }

      booksDF
        .primaryKey('id)
        .notNull('title)
        .unique('title)
      match {
        case cd @ ConstrainedDataset(name, data, violations) =>
          assert(name == "books")
          assert(violations.size == 0)
          cd.showViolations
      }

      bookAuthorsDF
        .primaryKey('people_id, 'book_id)
        .foreignKey('people_id).references(peopleDF).at('id)
        .foreignKey('book_id) references booksDF at 'id
      match {
        case cd @ ConstrainedDataset(name, data, violations) =>
          assert(name == "book_authors")
          assert(violations.size == 1)
          cd.showViolations
      }
    }
  }
}
