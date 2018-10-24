package scalajswebsocket


import fs2._
import cats.effect._
import cats.effect.IO._
import scalajswebsocket.WebSocketFrame._
import cats.effect.implicits._
import org.scalatest._

import scala.concurrent._
import scala.scalajs.concurrent._

class EchoServiceIntegrationSpec extends AsyncFunSpec {

  implicit override def executionContext = JSExecutionContext.Implicits.queue
  implicit val contextShift = IO.contextShift(executionContext)
  implicit val timer = IO.timer(executionContext)

  it("Echo 3 then Close") {
    val program = for {
      socket <- WebSocketClient.connectOrFail[IO]("ws://localhost:8079/testws")
      _ <- socket.write1(Text("Hello"))
      _ <- Stream("from", "scalajswebsocket").map(Text(_)).through(socket.write).compile.drain
      _ <- socket.write1(Close())
      replies <- socket.read.take(4).compile.toList
    } yield
      assertResult(List[WebSocketFrame](Text("Hello"), Text("from"), Text("scalajswebsocket"), Close()))(replies)

    program.unsafeToFuture()
  }

}
