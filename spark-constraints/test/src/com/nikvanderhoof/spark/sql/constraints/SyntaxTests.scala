package com.nikvanderhoof.spark.sql.constraints

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{expr, col}

import utest._

object SyntaxTests extends TestSuite with UtestSparkSession {

  import spark.implicits._
  import syntax._

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

    def checkConstraintsSize(df: DataFrame, size: Int) =
      assert(df.constraints.size == size)

    def checkConstraintsBeforeAndAfterDrop(df: DataFrame, size: Int) = {
      checkConstraintsSize(df, size)
      df.dropAllConstraints
      assert(df.constraints.size == 0)
    }

    "addConstraint syntax" - {
      peopleDF
        .addConstraint("PK", primaryKey('id))
        .addConstraint("ageCheck", check(col("age") >= 0 && col("age") < 120))
        .addConstraint("nameNotNull", notNull(col("name")))
        .addConstraint("ageNotNull", notNull('age))

      booksDF
        .addConstraint("PK", primaryKey('id))
        .addConstraint("titleNotNull", notNull('title))
        .addConstraint("uniqueTitle", unique('title))

      bookAuthorsDF
        .addConstraint("PK", primaryKey ('people_id, 'book_id))
        .addConstraint("FK_people", foreignKey('people_id) references peopleDF at 'id)
        .addConstraint("FK_books", foreignKey('people_id) references booksDF at 'id)

      checkConstraintsSize(peopleDF, 4)
      peopleDF.dropConstraint("ageCheck")
      checkConstraintsSize(peopleDF, 3)

      checkConstraintsSize(booksDF, 3)
      booksDF.dropConstraint("PK")
      checkConstraintsSize(booksDF, 2)

      checkConstraintsSize(bookAuthorsDF, 3)
      bookAuthorsDF.dropConstraint("FK_books")
      checkConstraintsSize(bookAuthorsDF, 2)
    }

  }

}
