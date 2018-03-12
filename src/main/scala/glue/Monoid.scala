package glue

trait Monoid[A] {
  def zero: A
  def combine(a1: A, a2: A): A
  def concat(as: List[A]): A = as.foldLeft(zero)(combine)
}