package scalacoin.types

object Miner {
  type Account = Int
  
  def mineOn(pendingTransactions: List[Transaction], minerAccount: Account, chain: Blockchain): Blockchain = {
    val block = Block(pendingTransactions)
    val header = BlockHeader(minerAccount, hash(chain))
    Node(header, block, chain)
  }

  def makeGenesis: Blockchain = Genesis(Block(List.empty))

  def hash(chain: Blockchain): String = {
    chain match {
      case Genesis(b) => Sha256.digest(b.toString)
      case Node(h, b, _) => Sha256.digest(s"$h.toString:$b.toString")
    }
  }
}