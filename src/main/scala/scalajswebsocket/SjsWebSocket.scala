package scalajswebsocket

import fs2.{Stream, Sink}
import java.net._

import cats.implicits._
import cats.effect._
import cats.effect.implicits._
import fs2.concurrent._
import fs2._
import fs2.Stream._
import org.scalajs.dom._

import scala.concurrent.duration._
import scala.scalajs.js.typedarray.ArrayBuffer

object WebSocketClient {

  val WebsocketOpenTimeout = 30 seconds

  def connect[F[_]](url: String)(implicit F: ConcurrentEffect[F], timer: Timer[IO]): F[Either[String, SjsWebSocket[F]]] = {
    val socket = new WebSocket(url)
    socket.binaryType = "arraybuffer"

    F.asyncF[Either[String, SjsWebSocket[F]]](k => for {
      q <- Queue.unbounded[F, WSFrame]
    } yield {
      val sjssocket = new SjsWebSocket(socket, q)
      socket.onopen = (e: Event) => {

        def handler: MessageEvent => F[Unit] = (e) =>
          (e.data match {
            case buf: ArrayBuffer => F.delay(Binary(buf))
            case s: String => F.delay(Text(s))
            case other => F.raiseError[WSFrame](new Exception(s"Unexpected WS message: $e"))
          }) >>=
            (q.enqueue1(_))

        socket.onmessage = (m) => {println("onmessage"); handler(m).toIO.unsafeRunAsyncAndForget()}
        //once open, further errors are treated as a Close on the queue of frames, there's little else to do but close the socket
        socket.onerror = (e: Event) => {println("onerror"); q.enqueue1(Close(1002, "onerror")).toIO.unsafeRunAsyncAndForget()}
        socket.onclose = (e: Event) => {println("onclose"); q.enqueue1(Close(1000, "onclose")).toIO.unsafeRunAsyncAndForget()}
        k(Right(Right(sjssocket)))
      }

      socket.onerror = (e: Event) => k(Right(Left(s"Websocket $url: onerror before onopen: $e")))
      socket.onclose = (e: Event) => k(Right(Left(s"Websocket $url: onclose before onopen: $e")))

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

class SjsWebSocket[F[_]](ws: WebSocket, queue: Queue[F, WSFrame])(implicit F: Sync[F]) {


  def close(code: Int, reason: String = "")= F.delay(ws.close(code, reason))

  def read1: F[WSFrame] = queue.dequeue1

  def read: Stream[F, WSFrame] =
    Stream.eval(read1).repeat

  def write1(msg: WSMessage): F[Unit] = msg match {
    case Text(data) => F.delay(ws.send(data))
    case Binary(data) => F.delay(ws.send(data))
  }

  def write: Sink[F, WSMessage] =
    Sink(write1)

  def url: String = ws.url
}
