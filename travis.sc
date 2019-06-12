import ammonite.ops._
import ammonite.ops.ImplicitWd._

def genTravisYml(crossMatrix: Seq[(String, String)]): String = s"""
language: scala
sudo: required
dist: trusty

git:
  depth: false

matrix:
  include:
${
  crossMatrix map { case (scala, spark) =>
    Seq(s"scala: $scala", s"env: SPARK=$spark")
  } flatMap { items => items.zipWithIndex map {
      case (x, i) if i == 0 => s"   - $x"
      case (x, i)           => s"     $x"
    }
  } mkString("\n")
}

script:
  - curl -L -o ~/bin/mill https://github.com/lihaoyi/mill/releases/download/0.4.0/0.4.0 && chmod +x ~/bin/mill
  - export PATH=~/bin/mill:$$PATH
  - mill "spark-constraints[$$TRAVIS_SCALA_VERSION,$$SPARK]"
cache:
  directories:
    - $$HOME/.coursier
""".trim + "\n"


def updateTravisYml(crossMatrix: Seq[(String, String)]): Unit = {
  val wd = pwd
  val travisYml = ".travis.yml"
  write.over(wd/travisYml, genTravisYml(crossMatrix))
  val diff = %%('git, "diff", travisYml).out.string
  if (diff.nonEmpty) {
    %%('git, "add", travisYml)
    %%('git, "commit", "-m", "Update .travis.yml")
  }
}
