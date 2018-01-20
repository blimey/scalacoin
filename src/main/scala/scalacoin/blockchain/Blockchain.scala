package scalacoin.blockchain

case class Block(index: Long, previousHash: String, timestamp: Long, data: String)

object Block {
  import scalacoin.crypto._

  def hash(block: Block): String =
    Sha256.digest(s"$block.index$block.previousHash$block.timestamp$block.data")

  def isValidBlock(block: Block, previousBlock: Block) =
    block.index == previousBlock.index + 1 && block.previousHash == hash(previousBlock)

  def isValidChain(blocks: List[Block]): Boolean =
    blocks.zip(blocks.tail).forall { case (block, previousBlock) => isValidBlock(block, previousBlock) }
}

class Blockchain(val blocks: List[Block]) {
  import Block._

  def lastBlock: Block = blocks.head

  def isValid: Boolean = isValidChain(blocks)

  def addBlock(block: Block): Blockchain =
    if (isValidBlock(block, lastBlock)) Blockchain(block :: blocks) else this

  def createBlock(data: String): Block = {
    val previousBlock = lastBlock
    Block(previousBlock.index + 1, hash(previousBlock), System.currentTimeMillis / 1000, data)
  }
}

object Blockchain {
  val genesisBlock = Block(0, "", System.currentTimeMillis / 1000, "Genesis Block")

  def apply(): Blockchain = new Blockchain(List(genesisBlock))

  def apply(blocks: List[Block]): Blockchain = new Blockchain(blocks)
}