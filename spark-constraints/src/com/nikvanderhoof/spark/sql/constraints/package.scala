package com.nikvanderhoof.spark.sql

import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

import com.github.dwickern.macros.NameOfImpl
import org.apache.spark.sql.{Column, Dataset}
import org.apache.spark.sql.constraints.datasetToNamedDataset
import org.apache.spark.sql.functions.col

package object constraints {
  implicit def dataset2NamedDataset[T](dataset: Dataset[T]) = datasetToNamedDataset(dataset)

  def constrain[T](dataset: Dataset[T])(block: Any): ConstrainedDataset[T] = macro constrainImpl[T]

  def constrainImpl[T](c: Context)(dataset: c.Tree)(block: c.Tree): c.Tree = {
    import c.universe._
    def nameOf(t: c.Tree) = NameOfImpl.nameOf(c)(c.Expr(t)).tree
    val thisPackage = q"com.nikvanderhoof.spark.sql.constraints"

    val name = nameOf(dataset)
    val namedDataset = q"NamedDataset($name, $dataset)"
    val q"..$constraintSyntaxes" = block
    try {
      val constraintExprs = constraintSyntaxes.map {
        case q"$thisPackage.foreignKey(..$columns).references[$u]($refDataset).at(..$refColumns)" => {
          val refName = nameOf(refDataset)
          val namedRefDataset = q"NamedDataset($refName, $refDataset)"
          q"ForeignKey($namedDataset, $columns, $namedRefDataset, $refColumns)"
        }
        case q"$thisPackage.primaryKey(..$columns)" => q"PrimaryKey($namedDataset, $columns)"
        case q"$thisPackage.notNull(..$columns)" => q"NotNull($namedDataset, $columns)"
        case q"$thisPackage.unique(..$columns)" => q"Unique($namedDataset, $columns)"
        case q"$thisPacakge.check($column)" => q"Check($namedDataset, $column)"
      }
      return q"ConstrainedDataset($namedDataset, $constraintExprs)"
    } catch {
      case e: MatchError => throw new java.lang.IllegalArgumentException(s"Invalid syntax in constrain block.: $block")
    }
  }

  @compileTimeOnly("foreignKey syntax only available in a constrain(...) block")
  def foreignKey(columns: Column*) = new {
    def references[U](refDs: Dataset[U]) = new {
      def at(refColumns: Column*) = {}
    }
  }
  @compileTimeOnly("primaryKey syntax only available in a constrain(...) block")
  def primaryKey(columns: Column *) = {}
  @compileTimeOnly("unique syntax only available in a constrain(...) block")
  def unique(columns: Column *) = {}
  @compileTimeOnly("notNull syntax only available in a constrain(...) block")
  def notNull(columns: Column *) = {}
  @compileTimeOnly("check syntax only available in a constrain(...) block")
  def check(column: Column) = {}

  private[constraints] def columnsToStringRep(columns: Seq[Column]) = {
    if (columns.size > 1) {
      columns.mkString("(", ", ", ")")
    } else {
      columns.mkString(",")
    }
  }
}
