package scalacoin.types

import scalacoin.mining.Miner.Account

import glue._
import Glue._

case class BlockHeader(miner: Account, parentHash: String, nonce: Int, timestamp: Long)

case class Block[A](transactions: List[A])

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
