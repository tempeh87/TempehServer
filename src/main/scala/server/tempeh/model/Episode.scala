package server.tempeh.model

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import com.google.gson.Gson
import server.tempeh.category.DataSource

case class Episode(title: String, url: String, transcript: String, audioSource: String, dataSource: DataSource)

object Episode {
  //We are not really taking serious on encryption, we encrypt this just for preventing being searched easily.
  private val key: String = "May the Father of Understanding guide us.".take(16)
  val aesKey = new SecretKeySpec(key.getBytes(), "AES")
  val cipher = Cipher.getInstance("AES")

  /**
    * Not thread-safe!!!
    **/
  def encrypt(episode: Episode): String = {
    val json = new Gson().toJson(episode)
    cipher.init(Cipher.ENCRYPT_MODE, aesKey)
    val encrypted = cipher.doFinal(json.getBytes("UTF-8"))
    new sun.misc.BASE64Encoder().encode(encrypted)
  }
  /**
    * Not thread-safe!!!
    **/
  def decrypt(encrypted: String): Episode = {
    cipher.init(Cipher.DECRYPT_MODE, aesKey)
    val dec = new sun.misc.BASE64Decoder().decodeBuffer(encrypted)
    val decrypted = new String(cipher.doFinal(dec), "UTF-8")
    new Gson().fromJson(decrypted, classOf[Episode])
  }
}
