package scalacoin.currency

case class TransactionOutput(address: String, amount: Int)

case class TransactionInput(txOutputId: String, txOutputIndex: Int, signature: String)

case class Transaction(id: String, inputs: List[TransactionInput], outputs: List[TransactionOutput])
