package scalacoin.blockchain

import scala.math.{pow, max}

import cats.syntax.either._

case class Block(index: Long, hash: String, previousHash: String, timestamp: Long, data: String, difficulty: Int, nonce: Int)

object GenesisBlock extends Block(0, "a80681a5f23898676719ba5ecee32621c9e8844088173a7d3178f2165e5ca57d", "", 1514764800, "Genesis Block", 0, 0)

object Block {
  import scalacoin.crypto._

  def hash(block: Block): String =
    hash(block.index, block.previousHash, block.timestamp, block.data, block.difficulty, block.nonce)

  def hash(index: Long, previousHash: String, timestamp: Long, data: String, difficulty: Int, nonce: Int): String =
    Sha256.digest(s"$index:$previousHash:$timestamp:$data:$difficulty:$nonce")
}

class Blockchain private(val blocks: List[Block]) {
  import Block._
  import Blockchain._

  def lastBlock: Block = blocks.head

  def isValid: Boolean = isValidChain(blocks)

  def addBlock(data: String): Blockchain = new Blockchain(createNextBlock(data) :: blocks)

  def addBlock(block: Block): Either[Exception, Blockchain] =
    if (isValidBlock(block, lastBlock)) Right(new Blockchain(block :: blocks))
    else Left(new IllegalArgumentException("Invalid block added."))

  def createNextBlock(data: String): Block = {
    val index = lastBlock.index + 1
    val previousHash = lastBlock.hash
    val timestamp = currentTimestamp
    val difficulty = currentDifficulty

    def hashMatchesDifficulty(hash: String, difficulty: Int): Boolean = {
      // Quick and dirty solution (TODO: improve this):
      // Hash parameter is a 64 character string that 
      // converted in binary should be a 256 character long string.
      // BigInt.toString removes leading 0s, so check if difficulty is less or equal to removed leading 0s.
      val hexToBinary = BigInt(hash, 16).toString(2)
      256 - hexToBinary.length >= difficulty
    }

    @annotation.tailrec
    def proofOfWork(nonce: Int): (String, Int) = {
      val candidateHash = hash(index, previousHash, timestamp, data, difficulty, nonce)
      if (hashMatchesDifficulty(candidateHash, difficulty)) (candidateHash, nonce)
      else proofOfWork(nonce + 1)
    }

    val (successfulHash, nonce) = proofOfWork(0)

    Block(index, successfulHash, previousHash, timestamp, data, difficulty, nonce)
  }

  def replaceWith(newBlocks: List[Block]): Either[Exception, Blockchain] =
    if (isValidChain(newBlocks) && chainAccumulatedDifficulty(newBlocks) > chainAccumulatedDifficulty(blocks))
      Right(new Blockchain(newBlocks))
    else Left(new IllegalArgumentException("Invalid chain specified."))

  def replaceWith(chain: Blockchain): Either[Exception, Blockchain] = replaceWith(chain.blocks)

  def currentDifficulty: Int = {
    if (lastBlock.index % DifficultyAdjustmentIntervalInBlocks == 0 && lastBlock.index != 0) adjustedDifficulty
    else lastBlock.difficulty
  }

  def adjustedDifficulty: Int = {
    val lastAdjustmentBlock: Block = blocks.lift(DifficultyAdjustmentIntervalInBlocks).getOrElse(GenesisBlock)
    val timeExpectedInSecs: Long = BlockGenerationIntervalInSecs * DifficultyAdjustmentIntervalInBlocks
    val timeTakenInSec: Long = lastBlock.timestamp - lastAdjustmentBlock.timestamp

    val difficulty: Int = if (timeTakenInSec < timeExpectedInSecs / 2) lastAdjustmentBlock.difficulty + 1
      else if (timeTakenInSec > timeExpectedInSecs * 2) lastAdjustmentBlock.difficulty - 1
      else lastAdjustmentBlock.difficulty

    max(0, difficulty)
  }

  def accumulatedDifficulty: Int = chainAccumulatedDifficulty(blocks)
}

object Blockchain {
  import Block._

  val BlockGenerationIntervalInSecs: Int = 10
  val DifficultyAdjustmentIntervalInBlocks: Int = 10

  def apply(): Blockchain = new Blockchain(List(GenesisBlock))

  def isValidBlock(block: Block, previousBlock: Block) =
    block.index == previousBlock.index + 1 && 
      block.previousHash == previousBlock.hash &&
      isValidTimestamp(block, previousBlock) &&
      block.hash == hash(block)

  def isValidTimestamp(block: Block, previousBlock: Block) =
    previousBlock.timestamp - 60 < block.timestamp && block.timestamp - 60 < currentTimestamp

  def isValidChain(blocks: List[Block]): Boolean =
    blocks.zip(blocks.tail).forall { case (block, previousBlock) => isValidBlock(block, previousBlock) }

  def chainAccumulatedDifficulty(blocks: List[Block]): Int = blocks.map(_.difficulty).map(pow(2, _)).sum.toInt

  def currentTimestamp: Long = System.currentTimeMillis / 1000

  object Implicits {
    import io.circe.{ Decoder, Encoder, HCursor, Json, DecodingFailure }
    import io.circe.generic.semiauto._
    import io.circe.syntax._

    implicit val blockDecoder: Decoder[Block] = deriveDecoder
    implicit val blockEncoder: Encoder[Block] = deriveEncoder

    implicit val blockchainEncoder: Encoder[Blockchain] = new Encoder[Blockchain] {
      final def apply(a: Blockchain): Json = Json.obj(("blocks", Json.fromValues(a.blocks.map(_.asJson))))
    }

    implicit val blockchainDecoder: Decoder[Blockchain] = new Decoder[Blockchain] {
      final def apply(c: HCursor): Decoder.Result[Blockchain] =
        for {
          parsedBlocks <- c.downField("blocks").as[List[Block]]
        } yield new Blockchain(parsedBlocks)
    }
  }
}