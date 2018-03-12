package scalacoin.blockchain

import scala.math.{pow, max}

import scalacoin.types._

case class Block(index: Long, hash: String, previousHash: String, timestamp: Long, data: String, difficulty: Long, nonce: Long)

object GenesisBlock extends Block(0, "a80681a5f23898676719ba5ecee32621c9e8844088173a7d3178f2165e5ca57d", "", 1514764800, "Genesis Block", 0, 0)

case class Blockchain private (blocks: List[Block])

object Blockchain {
  val BlockGenerationIntervalInSecs: Int = 10
  val DifficultyAdjustmentIntervalInBlocks: Int = 10

  def apply(): Blockchain = new Blockchain(List(GenesisBlock))

  // Make this constructor private so caller can't instantiate invalid blockchains
  private def apply(blocks: List[Block]): Blockchain = new Blockchain(blocks)

  def isValidBlock(block: Block, previousBlock: Block) =
    block.index == previousBlock.index + 1 && 
      block.previousHash == previousBlock.hash &&
      isValidTimestampForBlock(block, previousBlock) &&
      block.hash == hashForBlock(block)

  def hashForBlock(block: Block): String =
    hashForBlock(block.index, block.previousHash, block.timestamp, block.data, block.difficulty, block.nonce)

  def lastBlock(chain: Blockchain): Block = chain.blocks.head

  def addBlock(chain: Blockchain, data: String): Blockchain = Blockchain(generateNextBlock(chain, data) :: chain.blocks)

  def addBlock(chain: Blockchain, block: Block): Blockchain = 
    if (isValidBlock(block, lastBlock(chain))) Blockchain(block :: chain.blocks)
    else chain

  def isValidTimestampForBlock(block: Block, previousBlock: Block) =
    previousBlock.timestamp - 60 < block.timestamp && block.timestamp - 60 < currentTimestamp

  def generateNextBlock(chain: Blockchain, data: String): Block = {
    val previousBlock: Block = lastBlock(chain)
    val index: Long = previousBlock.index + 1
    val previousHash: String = previousBlock.hash
    val timestamp: Long = currentTimestamp
    val difficulty: Long = currentDifficulty(chain)

    def hashMatchesDifficulty(hash: String, difficulty: Long): Boolean = {
      // Quick and dirty solution (TODO: improve this):
      // Hash parameter is a 64 character string that 
      // converted in binary should be a 256 character long string.
      // BigInt.toString removes leading 0s, so check if difficulty is less or equal to removed leading 0s.
      val hexToBinary = BigInt(hash, 16).toString(2)
      256 - hexToBinary.length >= difficulty
    }

    @annotation.tailrec
    def proofOfWork(nonce: Long): (String, Long) = {
      val candidateHash = hashForBlock(index, previousHash, timestamp, data, difficulty, nonce)
      if (hashMatchesDifficulty(candidateHash, difficulty)) (candidateHash, nonce)
      else proofOfWork(nonce + 1)
    }

    val (successfulHash, nonce) = proofOfWork(0)

    Block(index, successfulHash, previousHash, timestamp, data, difficulty, nonce)
  }

  def currentDifficulty(chain: Blockchain): Long = {
    val block: Block = lastBlock(chain)
    if (block.index % DifficultyAdjustmentIntervalInBlocks == 0 && block.index != 0) adjustedDifficulty(chain)
    else block.difficulty
  }

  def adjustedDifficulty(chain: Blockchain): Long = {
    val block: Block = lastBlock(chain)
    val lastAdjustmentBlock: Block = chain.blocks.lift(DifficultyAdjustmentIntervalInBlocks).getOrElse(GenesisBlock)
    val timeExpectedInSecs: Long = BlockGenerationIntervalInSecs * DifficultyAdjustmentIntervalInBlocks
    val timeTakenInSec: Long = block.timestamp - lastAdjustmentBlock.timestamp

    val difficulty: Long = if (timeTakenInSec < timeExpectedInSecs / 2) lastAdjustmentBlock.difficulty + 1
      else if (timeTakenInSec > timeExpectedInSecs * 2) lastAdjustmentBlock.difficulty - 1
      else lastAdjustmentBlock.difficulty

    max(0, difficulty)
  }

  def isValid(chain: Blockchain): Boolean = areValidBlocks(chain.blocks)
    
  def adjustedLength(chain: Blockchain): Int = chain.blocks.map(_.difficulty).map(pow(2, _)).sum.toInt

  def selectLongest(b1: Blockchain, b2: Blockchain): Blockchain =
    if (adjustedLength(b2) > adjustedLength(b1)) b2
    else b1

  @annotation.tailrec
  private def areValidBlocks(blocks: List[Block]): Boolean = blocks match {
    case last :: Nil if last == GenesisBlock => true
    case block :: previousBlock :: tail if isValidBlock(block, previousBlock) => areValidBlocks(previousBlock :: tail)
    case _ => false
  }

  private def hashForBlock(index: Long, previousHash: String, timestamp: Long, data: String, difficulty: Long, nonce: Long): String =
    Sha256.digest(s"$index:$previousHash:$timestamp:$data:$difficulty:$nonce")

  private def currentTimestamp: Long = System.currentTimeMillis / 1000

  object Implicits {
    import io.circe.{ Decoder, Encoder, HCursor, Json, DecodingFailure, CursorOp }
    import io.circe.generic.semiauto._
    import io.circe.syntax._

    implicit val blockDecoder: Decoder[Block] = deriveDecoder
    implicit val blockEncoder: Encoder[Block] = deriveEncoder

    implicit val blockchainEncoder: Encoder[Blockchain] = new Encoder[Blockchain] {
      final def apply(a: Blockchain): Json = Json.obj(("blocks", Json.fromValues(a.blocks.map(_.asJson))))
    }

    implicit val blockchainDecoder: Decoder[Blockchain] = new Decoder[Blockchain] {
      final def apply(c: HCursor): Decoder.Result[Blockchain] =
        (for {
          parsedBlocks <- c.downField("blocks").as[List[Block]]
        } yield parsedBlocks) match {
          case Left(error) => Left(error)
          case Right(blocks) =>
            // Need to verify blocks are valid too
            if (areValidBlocks(blocks)) Right(Blockchain(blocks))
            else Left(DecodingFailure("Invalid blocks", List(CursorOp.Field("blocks"))))
        }

    }
  }
}