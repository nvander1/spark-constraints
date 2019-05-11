package org.apache.spark.sql

import org.apache.spark.sql.catalyst.AliasIdentifier
import org.apache.spark.sql.catalyst.plans.logical.SubqueryAlias
import com.nikvanderhoof.spark.sql.constraints.NamedDataset

package object constraints {
  implicit def datasetToNamedDataset[T](dataset: Dataset[T]): NamedDataset[T] = {
    dataset.logicalPlan match {
      case SubqueryAlias(AliasIdentifier(name, _), _) => NamedDataset(name, dataset)
      case _ => NamedDataset("UNNAMED DATASET", dataset)
    }
  }
}
