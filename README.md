# Spark Constraints

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

### Example

```scala
...
import com.nikvanderhoof.spark.sql.constraints.syntax._
import spark.implicits._ // so we can specify columns via Symbols

val peopleDF: DataFrame = ...
val friendsDF: DataFrame = ...

peopleDF
  .addConstraint(primaryKey('id))
  .addConstraint(check(expr("age >= 0")))
  .addConstraint(notNull('name, 'age))

peopleDF.constraints.foreach(println)
```

You can specify names for constraints if you don't want the auto generated ones.

```scala
friendsDF
  .addConstraint("PK", primaryKey('person_a, 'person_b))
  .addConstraint("FK_a", foreignKey('person_a) references peopleDF at 'id)
  .addConstraint("FK_b", foreignKey('person_b) references peopleDF at 'id)

friendsDF.constraints.foreach(println)
```

## Related Work

Check out [spark-daria](https://github.com/MrPowers/spark-daria) if you need
help ensuring your datasets have the correct schema (among other cool features!).
Specifically, look into the [dataframe validators](https://github.com/MrPowers/spark-daria#dataframe-validators).
Those will help you make sure your datasets have all the right columns and types.
