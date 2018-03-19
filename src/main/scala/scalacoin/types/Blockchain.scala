package scalacoin.types

import glue._
import Glue._

import scala.language.higherKinds

object Types {
  type Account = Int

  case class Transaction(from: Account, to: Account, amount: Int)

  case class BlockHeader(miner: Account, parentHash: String)

  case class Block[A](transactions: List[A])

  sealed trait Blockchain[A]
  case class Genesis[A](block: A) extends Blockchain[A]
  case class Node[A](block: A, header: BlockHeader, next: Blockchain[A]) extends Blockchain[A]

  object Block {
    implicit def blockIsMonoid[A]: Monoid[Block[A]] = new Monoid[Block[A]] {
      def unit: Block[A] = Block(List.empty)
      def combine(b1: Block[A], b2: Block[A]): Block[A] = Block(b1.transactions ++ b2.transactions)
    }

    implicit val blockIsFoldable: Foldable[Block] = new Foldable[Block] {
      def foldLeft[A, B](b: Block[A])(z: B)(f: (B, A) => B): B = b.transactions.foldLeft(z)(f)

      def foldRight[A, B](b: Block[A])(z: B)(f: (A, B) => B): B = b.transactions.foldRight(z)(f)

      def foldMap[A, B](b: Block[A])(f: A => B)(implicit M: Monoid[B]): B =
        b.transactions.foldMap(f)
    }
  }

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
}
