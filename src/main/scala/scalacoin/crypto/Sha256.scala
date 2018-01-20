package scalacoin.crypto

import java.security.MessageDigest

object Sha256 {
  def digest(s: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(s.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
}