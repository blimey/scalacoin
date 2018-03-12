package glue

import scala.language.higherKinds
import scala.language.postfixOps

// TODO
trait Foldable[F[_]] {
  def foldRight[A, B](as: F[A])(z: B)(f: (A, B) => B): B
  def foldLeft[A, B](as: F[A])(z: B)(f: (B, A) => B): B
  def foldMap[A, B](as: F[A])(f: A => B)(implicit m: Monoid[B]): B
  def concatenate[A](as: F[A])(m: Monoid[A]): A = foldLeft(as)(m.zero)(m.combine)
  def toList[A](fa: F[A]): List[A] = foldLeft(fa)(List[A]())((b, a) => a :: b) reverse
}