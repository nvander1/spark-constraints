# Spark Constraints <img src=./logo.png alt="spark-constraints" width="64">

SQL-like constraints for your Spark datasets!

[![Build Status](https://travis-ci.org/nvander1/spark-constraints.svg?branch=develop)](https://travis-ci.org/nvander1/spark-constraints)

## Introduction

Schemas and case classes let you limit the types of data stored in a
DataFrame/Dataset. However, you may find you need more fine control
over the types of values you want in your dataset. For example, if
you are maintaining a dataset of people, all of their ages should
probably be positive--just having a schema is not enough to ensure
that constraint holds.

Loading data that doesn't meet your database constraints can be really annoying with Spark.  Spark will let you try to load anything in your database, but your database will throw an error if a validation is not met.

Suppose you're trying to load 2 million rows in a database and one row fails a database validation.  Spark will let you load all the data up to the validation error.  So you might load 1.2 million rows and then your job fails so the other 800,000 rows aren't loaded.

This puts you in the uncomfortable position of either having to figure out what rows weren't loaded or rolling back the 1.2 million rows that were loaded in the database.

spark-constraints saves you from fighting with database validations when loading data from Spark into a database.

## Getting Started

Use the following to install spark-constraints in an ammonite repl:
```scala
import $ivy.`com.nikvanderhoof::spark-constraints:0.1.0_spark2.4`
```

The following in a Mill build:
```scala
ivy"com.nikvanderhoof::spark-constraints:0.1.0_spark2.4"
```

And the following in Sbt:
```scala
"com.nikvanderhoof" %% "spark-constraints" % "0.1.0_spark2.4"
```

### Example

```scala
...
import com.nikvanderhoof.spark.sql.constraints._
import spark.implicits._ // so we can specify columns via Symbols

val peopleDF = Seq(
  (1, "Alice", 25),
  (2, "Bob", 20),
  (3, "Carol", -1)    // A negative age we can catch
).toDF("id", "name", "age")
  .as("people")

val booksDF = Seq(
  (1, "Introduction to Programming"),
  (2, "Number Systems"),
  (3, "Partial Differential Equations")
).toDF("id", "title")
  .as("books")

val bookAuthorsDF = Seq(
  (1, 1),
  (2, 2),
  (3, 4)    // Let's add a foreign key violation for demonstration
).toDF("people_id", "book_id")
  .as("book_authors")

val peopleResults = peopleDF
  .withPrimaryKey('id)
  .withCheck(col("age") >= 0 && col("age") < 120)
  .withNotNull(col("name"))
  .withNotNull('age)

val booksResults = booksDF
  .withPrimaryKey('id)
  .withNotNull('title)
  .withUnique('title)

val bookAuthorsResults = bookAuthorsDF
  .withPrimaryKey('people_id, 'book_id)
  .withForeignKey('people_id) to peopleDF at 'id
  .withForeignKey('people_id) to booksDF at 'id

peopleResults.showViolations
booksResults.showViolations
bookAuthorsResults.showViolations
```

Will print out the following information:

```
Check(people, ((age >= 0) AND (age < 120)))
+---+-----+---+
| id| name|age|
+---+-----+---+
|  3|Carol| -1|
+---+-----+---+

ForeignKey(book_authors, book_id, books, id)
+---------+-------+
|people_id|book_id|
+---------+-------+
|        3|      4|
+---------+-------+
```

## Building and Testing
The project is built with [mill](https://github.com/lihaoyi/mill).
spark-constraints supports the following versions of Scala/Spark:

- Scala 2.11.8 / Spark 2.3.0
- Scala 2.11.8 / Spark 2.4.0
- Scala 2.12.4 / Spark 2.4.0

Other verisons may work, but are not officially supported.

To build the project and run the tests, make sure you have mill installed
and run the following at the project root:

```
mill __.test
```

To compile/test just a specific version use:

```
mill "spark-constraints[2.11.8,2.3.0].compile"
mill "spark-constraints[2.11.8,2.3.0].test"
```

## Related Work

Check out [spark-daria](https://github.com/MrPowers/spark-daria) if you need
help ensuring your datasets have the correct schema (among other cool features!).
Specifically, look into the [dataframe validators](https://github.com/MrPowers/spark-daria#dataframe-validators).
Those will help you make sure your datasets have all the right columns and types.
