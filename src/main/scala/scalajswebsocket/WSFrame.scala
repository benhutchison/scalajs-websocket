package scalajswebsocket

import java.nio.charset.StandardCharsets._

import scala.util.hashing._
import scodec.bits.ByteVector

import scala.scalajs._
import scala.scalajs.js.typedarray.TypedArrayBuffer

sealed trait WSFrame

sealed trait WSMessage extends WSFrame

case class Binary(buffer: js.typedarray.ArrayBuffer) extends WSMessage {

  def asByteVector: ByteVector = ByteVector.view(TypedArrayBuffer.wrap(buffer))

}

case class Text(text: String) extends WSMessage

case class Close(code: Int, reason: String = "") extends WSFrame



