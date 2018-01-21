package scalacoin.blockchain

import cats.syntax.either._

case class Block(index: Long, previousHash: String, timestamp: Long, data: String)

object GenesisBlock extends Block(0, "0", System.currentTimeMillis / 1000, "Genesis Block")

object Block {
  import scalacoin.crypto._

  def hash(block: Block): String =
    Sha256.digest(s"$block.index:$block.previousHash:$block.timestamp:$block.data")
}

class Blockchain(val blocks: List[Block]) {
  import Block._
  import Blockchain._

  def lastBlock: Block = blocks.head
  def firstBlock: Block = blocks.last

  def isValid: Boolean = isValidChain(blocks)

  def addBlock(data: String): Blockchain = new Blockchain(createNextBlock(data) :: blocks)

  def addBlock(block: Block): Either[Exception, Blockchain] =
    if (isValidBlock(block, lastBlock)) Right(new Blockchain(block :: blocks))
    else Left(new IllegalArgumentException("Invalid block added."))

  def createNextBlock(data: String): Block = {
    val previousBlock = lastBlock
    Block(previousBlock.index + 1, hash(previousBlock), System.currentTimeMillis / 1000, data)
  }
}

object Blockchain {
  import Block._

  def apply(): Blockchain = new Blockchain(List(GenesisBlock))

  def apply(blocks: List[Block]): Either[Exception, Blockchain] =
    if (isValidChain(blocks)) Right(new Blockchain(blocks))
    else Left(new IllegalArgumentException("Invalid chain specified."))

  def isValidBlock(block: Block, previousBlock: Block) =
    block.index == previousBlock.index + 1 && block.previousHash == hash(previousBlock)

  def isValidChain(blocks: List[Block]): Boolean =
    blocks.zip(blocks.tail).forall { case (block, previousBlock) => isValidBlock(block, previousBlock) }
}