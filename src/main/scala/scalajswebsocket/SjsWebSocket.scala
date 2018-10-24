package scalajswebsocket

import fs2.{Stream, Sink}
import java.net._

import cats.implicits._
import cats.effect._
import cats.effect.implicits._
import fs2.concurrent.Queue
import fs2._
import fs2.Stream._



import org.scalajs.dom._
import scalajswebsocket.WebSocketFrame._

import scala.concurrent.duration._

object WebSocketClient {

  val WebsocketOpenTimeout = 30 seconds

  def connect[F[_]: ConcurrentEffect](url: String)(implicit timer: Timer[IO]): F[Either[String, SjsWebSocket[F]]] = {
    val socket = new WebSocket(url)
    socket.binaryType = "arraybuffer"

    Async[F].asyncF[Either[String, SjsWebSocket[F]]](k => for {
      q <- Queue.unbounded[F, WebSocketFrame]
    } yield {
      val sjssocket = new SjsWebSocket(socket, q)
      socket.onopen = (e: Event) => {
        //once open, further errors are treated as a Close on the queue of frames, there's little else to do but close the socket
        socket.onerror = (e: Event) => sjssocket.close(1002, "onerror")
        socket.onclose = (e: Event) => sjssocket.close(1000, "onclose")
        k(Right(Right(sjssocket)))
      }

      socket.onerror = (e: Event) => k(Right(Left(s"Websocket $url: onerror before onopen: $e")))
      socket.onclose = (e: Event) => k(Right(Left(s"Websocket $url: onclose before onopen: $e ")))

      timer.sleep(WebsocketOpenTimeout).map(
        _ => k(Right(Left(s"Websocket $url: timeout after $WebsocketOpenTimeout")))).unsafeRunAsyncAndForget()
      ()
    })
  }

  def connectOrFail[F[_]: ConcurrentEffect](url: String)(implicit timer: Timer[IO]): F[SjsWebSocket[F]] = for {
    tryConnect <- connect(url)
    socket <- Sync[F].fromEither(tryConnect.leftMap(new Exception(_)))
  } yield socket

}

class SjsWebSocket[F[_]: ConcurrentEffect](ws: WebSocket, queue: Queue[F, WebSocketFrame]) {

  def close(code: Int, reason: String)= Close(1000, reason).foreach(queue.enqueue1(_).toIO.unsafeRunAsyncAndForget())

  def read1: F[WebSocketFrame] = queue.dequeue1

  def read: Stream[F, WebSocketFrame] =
    Stream.eval(read1).repeat

  def write1(frame: WebSocketFrame): F[Unit] = queue.enqueue1(frame)

  def write: Sink[F, WebSocketFrame] =
    Sink(write1)

  def url: String = ws.url
}
