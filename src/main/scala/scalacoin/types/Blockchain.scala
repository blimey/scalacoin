package scalacoin.types

import Miner._

case class Transaction(from: Account, to: Account, amount: Int)

case class BlockHeader(miner: Account, parentHash: String)

case class Block(transactions: List[Transaction])

sealed trait Blockchain
case class Genesis(block: Block) extends Blockchain
case class Node(header: BlockHeader, block: Block, tail: Blockchain) extends Blockchain
