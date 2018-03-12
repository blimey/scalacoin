package scalacoin.types

case class Account(address: String)

case class Transaction(from: Account, to: Account, amount: Int)

case class BlockHeader(miner: Account, parentHash: String)

case class Block[+A](transactions: Vector[A])

object Block {
  import glue.{Monoid, Functor}

  implicit def blockIsMonoid[A]: Monoid[Block[A]] = new Monoid[Block[A]] {
    def zero: Block[A] = Block[A](Vector.empty)
    def combine(b1: Block[A], b2: Block[A]): Block[A] = Block(b1.transactions ++ b2.transactions)
  }

  implicit val blockIsFunctor: Functor[Block] = new Functor[Block] {
    def map[A, B](fa: Block[A])(f: A => B): Block[B] = Block(fa.transactions.map(f))
  }

  // TODO: BLock is also foldable and traversable, it can be shown and checked for equality
}

sealed trait Blockchain[+A]
case object Genesis extends Blockchain[Nothing]
case class Node[+A](header: BlockHeader, block: Block[A], tail: Blockchain[A]) extends Blockchain[A]

// TODO: Blockchain is a functor, it's traversable and foldable, it can be shown and checked for equality
