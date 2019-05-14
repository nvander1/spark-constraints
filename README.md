# Spark Constraints <img src=./logo.png alt="spark-constraints" width="64">

SQL-like constraints for your Spark datasets!

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
interp.repositories() ++=
  Seq(coursier.MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"))

import $ivy.`com.nikvanderhoof::spark-constraints:0.1.0_spark2.4-SNAPSHOT`
```

The following in a Mill build:
```scala
def repositories = super.repositories ++
  Seq(coursier.MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"))

ivy"com.nikvanderhoof::spark-constraints:0.1.0_spark2.4-SNAPSHOT"
```

And the following in Sbt:
```scala
resolvers += "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

"com.nikvanderhoof" %% "spark-constraints" % "0.1.0_spark2.4-SNAPSHOT"
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

val booksDF = Seq(
  (1, "Introduction to Programming"),
  (2, "Number Systems"),
  (3, "Partial Differential Equations")
).toDF("id", "title")

val bookAuthorsDF = Seq(
  (1, 1),
  (2, 2),
  (3, 4)    // Let's add a foreign key violation for demonstration
).toDF("people_id", "book_id")

val peopleResults = constrain(peopleDF) {
  primaryKey('id)
  check(col("age") >= 0 && col("age") < 120)
  notNull(col("name"))
  notNull('age)
}

val booksResults = constrain(booksDF) {
  primaryKey('id)
  notNull('title)
  unique('title)
}

val bookAuthorsResults = constrain(bookAuthorsDF) {
  primaryKey('people_id, 'book_id)
  foreignKey('people_id) references peopleDF at 'id
  foreignKey('people_id) references booksDF at 'id
}

peopleResults.showViolations
booksResults.showViolations
bookAuthorsResults.showViolations
```

Will print out the following information:

```
Check(peopleDF, ((age >= 0) AND (age < 120)))
+---+-----+---+
| id| name|age|
+---+-----+---+
|  3|Carol| -1|
+---+-----+---+

ForeignKey(bookAuthorsDF, book_id, booksDF, id)
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
./scripts/test.sh
```

To compile/test just a specific version use:

```
mill "spark-constraints[2.11.8,2.3.0].compile
mill "spark-constraints[2.11.8,2.3.0].test
```

## Related Work

Check out [spark-daria](https://github.com/MrPowers/spark-daria) if you need
help ensuring your datasets have the correct schema (among other cool features!).
Specifically, look into the [dataframe validators](https://github.com/MrPowers/spark-daria#dataframe-validators).
Those will help you make sure your datasets have all the right columns and types.
