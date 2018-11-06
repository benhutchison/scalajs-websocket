package scalajswebsocket


import fs2._
import cats.effect._
import cats.effect.IO._
import cats.effect.implicits._
import org.specs2.matcher.ResultMatchers
import org.specs2.mutable._

import scala.concurrent._
import scala.concurrent.duration._
import scala.scalajs.concurrent._
import scala.scalajs.concurrent.JSExecutionContext._

class EchoServiceIntegrationSpec extends Specification with ResultMatchers  {

  implicit val contextShift = IO.contextShift(queue)
  implicit val timer = IO.timer(queue)

  "Echo 3 then Close" in {
    val program = for {
      socket <- WebSocketClient.connectOrFail[IO]("ws://localhost:8079/testws")
      _ <- socket.write1(Text("Hello"))
      _ <- Stream("from", "scalajswebsocket").map(Text(_)).through(socket.write).compile.drain
      //websockets send/receive channels are decoupled so we must wait for replies before sending Close,
      //or else we might close the socket before the replies are sent
      replies <- socket.read.take(3).compile.toList
      _ <- socket.close(CloseCodeNormal)
      close <- socket.read.take(1).compile.toList
    } yield {
      List[WSFrame](Text("Hello"), Text("from"), Text("scalajswebsocket"), Close(CloseCodeNormal, "onclose")) ===
        (replies ++ close)
    }
    program.unsafeToFuture()
  }

}
