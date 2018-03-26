package scalacoin.mining

import scalacoin.types._
import scalacoin.types.{Blockchain => BlockchainF}

import glue._
import glue.prelude._

import scala.math.{min, round}
import scala.util.Try

object Miner {
  type Account = Int

  type Block = BlockF[Transaction]
  type Blockchain = BlockchainF[Block]

  val TransactionLimit: Int = 1000

  val BlockMiningReward: Int = 1000
  val BlockMiningTargetTimeInSecs: Double = 10.0

  val NumBlocksBeforeDifficultyAdjustment: Int = 10
  val DefaultDifficulty: BigInt = BigInt("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)  // 64 bytes

  def mineOn(pendingTransactions: List[Transaction], minerAccount: Account, parent: Blockchain): Blockchain = {
    val transactions: List[Transaction] = validTransactions(parent, pendingTransactions).take(TransactionLimit)
    val desiredDiff = desiredDifficulty(parent)

    @annotation.tailrec
    def loop(timestamp: Long, nonce: Int): Blockchain = {
      val header: BlockHeader = BlockHeader(minerAccount, hash(parent), nonce, timestamp)
      val block: Block = BlockF(transactions)
      val candidate: Blockchain = Node(block, header, parent)
      if (difficulty(candidate) < desiredDiff) candidate
      else loop(timestamp, nonce + 1)
    }

    loop(System.currentTimeMillis / 1000, 0)
  }

  def makeGenesis: Blockchain = Genesis(BlockF(List.empty))

  def balances(bc: Blockchain): Map[Account, Int] = {
    val txns: List[Transaction] = transactions(bc)

    val debits: List[(Account, Int)] = txns.map { case Transaction(from, _, amount) => (from, -amount) }
    val credits: List[(Account, Int)] = txns.map { case Transaction(_, to, amount) => (to, amount)}
    val minings: List[(Account, Int)] = headers(bc).map(h => (h.miner, BlockMiningReward))

    (debits ++ credits ++ minings).groupBy(_._1).mapValues(_.map(_._2).sum)
  }

  def transactions(bc: Blockchain): List[Transaction] = bc.toList.combine.toList

  def validTransactions(bc: Blockchain, transactions: List[Transaction]): List[Transaction] = {
    val accounts: Map[Account, Int] = balances(bc)
    transactions.filter { t =>
      accounts get t.from match {
        case None => false
        case Some(balance) => balance >= t.amount
      }
    }
  }

  def difficulty(bc: Blockchain): BigDecimal = BigDecimal(BigInt(hash(bc), 16))

  def desiredDifficulty(bc: Blockchain): BigDecimal = {
    def loop(bc: Blockchain): BigDecimal = bc match {
      case Genesis(_) => BigDecimal(DefaultDifficulty)
      case x @ Node(_, _, xs) => {
        val oldDesiredDifficulty: BigDecimal = loop(xs)
        val blockTimeAvg: BigDecimal = blockTimeAverage(x)
        val adjustmentFactor: BigDecimal = BigDecimal(4.0).min(if (blockTimeAvg == 0) BlockMiningTargetTimeInSecs else BlockMiningTargetTimeInSecs / blockTimeAvg)
        oldDesiredDifficulty / adjustmentFactor
      }
    }

    loop(bc)
  }

  def totalVolume(bc: Blockchain): Int = transactions(bc).map(_.amount).sum

  def headers(bc: Blockchain): List[BlockHeader] = {
    @annotation.tailrec
    def loop(x: Blockchain, acc: List[BlockHeader]): List[BlockHeader] = x match {
      case Genesis(_) => acc
      case Node(_, h, xs) => loop(xs, h :: acc)
    }

    loop(bc, List.empty)
  }

  def blockTimeAverage(bc: Blockchain): Double = {
    val times: List[Long] = headers(bc) map { _.timestamp } take NumBlocksBeforeDifficultyAdjustment
    val timeDiffs: List[Long] = zipWith_1(safeTail(times), times)(_ - _)
    val (sum, length): (Long, Int) = timeDiffs.foldLeft((0L, 0)) { case ((s, l), x) => (s + x, l + 1) }
    sum / (if (length == 0) 1.0 else length * 1.0)
  }

  def chains(bc: Blockchain): List[Blockchain] = {
    def loop(x: Blockchain, acc: List[Blockchain]): List[Blockchain] = x match {
      case y @ Genesis(_) => acc ::: List(y)
      case y @ Node(_, _ , xs) => loop(xs, y :: acc)
    }

    loop(bc, List.empty)
  }

  def addBlock(block: Block, header: BlockHeader, bc: Blockchain): Blockchain = Node(block, header, bc)

  def isValidChain(bc: Blockchain): Boolean = true

  def isValidBlock(block: Block, header: BlockHeader, bc: Blockchain): Boolean = true

  def isValidTransaction(bc: Blockchain, t: Transaction): Boolean = true

  // This is not stack safe, avoid usage, only for illustration purpouses
  def zipWith[A, B, C](as: List[A], bs: List[B])(f: (A, B) => C): List[C] = (as, bs) match {
    case (_, Nil) | (Nil, _) => Nil
    case (x :: xs, y :: ys) => f(x, y) :: zipWith(xs, ys)(f)
  }

  def zipWith_1[A, B, C](as: List[A], bs: List[B])(f: (A, B) => C): List[C] = {
    @annotation.tailrec
    def loop(l: List[A], r: List[B], acc: List[C]): List[C] =
      (l, r) match {
        case (_, Nil) | (Nil, _) => acc
        case (x :: xs, y :: ys) => loop(xs, ys, f(x, y) :: acc)
      }
    
    loop(as, bs, List.empty)
  }

  // Implementation using local mutation
  def zipWith_2[A, B, C](as: List[A], bs: List[B])(f: (A, B) => C): List[C] = {
    val buffer = new collection.mutable.ListBuffer[C]
    def loop(l: List[A], r: List[B]): Unit = (l, r) match {
      case (_, Nil) | (Nil, _) => ()
      case (x :: xs, y :: ys) => buffer += f(x, y); loop(xs, ys)
    }
    loop(as, bs)
    List(buffer.toList: _*)
  }

  def safeTail[A](as: List[A]): List[A] = if (as.isEmpty) List.empty else as.tail

  def hash(bc: Blockchain): String = {
    bc match {
      case Genesis(b) => Sha256.hash(b.toString)
      case Node(h, b, _) => Sha256.hash(s"$h.toString:$b.toString")
    }
  }
}