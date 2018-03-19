package scalacoin.types

import Types._
import Types.{Blockchain => BlockchainF}

import glue._
import Glue._

object Miner {
  type Blockchain = BlockchainF[Block[Transaction]]

  val BlockMiningReward: Int = 1000

  def mineOn(pendingTransactions: List[Transaction], minerAccount: Account, chain: Blockchain): Blockchain = {
    val block = Block(pendingTransactions)
    val header = BlockHeader(minerAccount, hash(chain))
    Node(block, header, chain)
  }

  def makeGenesis: Blockchain = Genesis(Block(List.empty))

  def balances(chain: Blockchain): Map[Account, Int] = {
    val txns: List[Transaction] = transactions(chain)

    val debits: List[(Account, Int)] = txns.map { case Transaction(from, _, amount) => (from, -amount) }
    val credits: List[(Account, Int)] = txns.map { case Transaction(_, to, amount) => (to, amount)}
    val minings: List[(Account, Int)] = headers(chain).map(h => (h.miner, BlockMiningReward))

    (debits ++ credits ++ minings).groupBy(_._1).mapValues(_.map(_._2).sum)
  }

  def transactions(chain: Blockchain): List[Transaction] = chain.toList.combine.toList

  def totalVolume(chain: Blockchain): Int = transactions(chain).map(_.amount).sum

  def headers(chain: Blockchain): List[BlockHeader] = {
    @annotation.tailrec
    def loop(current: Blockchain, acc: List[BlockHeader]): List[BlockHeader] = chain match {
      case Genesis(_) => acc
      case Node(_, h, t) => loop(t, h :: acc)
    }

    loop(chain, List.empty)
  }

  private def hash(chain: Blockchain): String = {
    chain match {
      case Genesis(b) => Sha256.hash(b.toString)
      case Node(h, b, _) => Sha256.hash(s"$h.toString:$b.toString")
    }
  }
}