package scalacoin.types

import scalacoin.mining.Miner.Account

case class Transaction(from: Account, to: Account, amount: Int)