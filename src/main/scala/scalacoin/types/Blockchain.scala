package scalacoin.types

import glue._
import glue.prelude._

import scala.language.higherKinds

sealed trait Blockchain[A]
case class Genesis[A](block: A) extends Blockchain[A]
case class Node[A](block: A, header: BlockHeader, next: Blockchain[A]) extends Blockchain[A]

object Blockchain {
  implicit val blockchainIsFoldable: Foldable[Blockchain] = new Foldable[Blockchain] {
    @annotation.tailrec
    def foldLeft[A, B](chain: Blockchain[A])(z: B)(f: (B, A) => B): B = chain match {
      case Genesis(b) => f(z, b)
      case Node(b, _, t) => foldLeft(t)(f(z, b))(f)
    }

    def foldRight[A, B](chain: Blockchain[A])(z: B)(f: (A, B) => B): B = {
      @annotation.tailrec
      def loop(current: Blockchain[A], acc: B): B = current match {
        case Genesis(b) => f(b, acc)
        case Node(b, _, t) => loop(t, f(b, acc))
      }

      loop(chain, z)
    }

    def foldMap[A, B](chain: Blockchain[A])(f: A => B)(implicit M: Monoid[B]): B =
      foldLeft(chain)(M.unit)((b, a) => M.combine(b, f(a)))
  }
}
