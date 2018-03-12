package glue

import scala.language.higherKinds

// TODO
trait Applicative[F[_]] extends Functor[F] {
  def unit[A](a: => A): F[A]
  def apply[A, B](fab: F[A => B])(fa: F[A]): F[B]

  def map[A, B](fa: F[A])(f: A => B): F[B] = ???
}