package scalacoin.types

import scalacoin.mining.Miner.Account

import glue._
import glue.prelude._

case class BlockHeader(miner: Account, parentHash: String, nonce: Int, timestamp: Long)

case class BlockF[A](transactions: List[A])

object BlockF {
  implicit def blockIsMonoid[A]: Monoid[BlockF[A]] = new Monoid[BlockF[A]] {
    val unit: BlockF[A] = BlockF(List.empty)
    def combine(b1: BlockF[A], b2: BlockF[A]): BlockF[A] = BlockF(b1.transactions ++ b2.transactions)
  }

  implicit val blockIsFoldable: Foldable[BlockF] = new Foldable[BlockF] {
    def foldLeft[A, B](b: BlockF[A])(z: B)(f: (B, A) => B): B = b.transactions.foldLeft(z)(f)

    def foldRight[A, B](b: BlockF[A])(z: B)(f: (A, B) => B): B = b.transactions.foldRight(z)(f)

    def foldMap[A, B](b: BlockF[A])(f: A => B)(implicit M: Monoid[B]): B =
      b.transactions.foldMap(f)
  }
}
