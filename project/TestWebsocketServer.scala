package scalajswebsocket.testsupport

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import fs2._
import fs2.concurrent._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.server.websocket._
import org.http4s.websocket._
import org.http4s.websocket.WebSocketFrame._

import scala.concurrent._
import scala.concurrent.duration._


object TestWebsocketServer extends Http4sDsl[IO]  {

  implicit val contextShift = IO.contextShift(ExecutionContext.global)
  implicit val timer = IO.timer(ExecutionContext.global)

  val Port = 8079

  def start(): Unit = {
    if (runningServer.isEmpty) {
      runningServer = Some(TestWebsocketServer.start(Port))
      println(s"started TestWebsocketServer on $Port: $runningServer")
    }
    else
      ()
  }

  def stop(): Unit = {
    println(s"stopping TestWebsocketServer on $Port")
    runningServer.foreach(_.cancel)
    runningServer = None
  }

  private var runningServer: Option[Fiber[IO, ExitCode]] = None

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "testws" =>
      val echoReply: Pipe[IO, WebSocketFrame, WebSocketFrame] =
        _.collect {
          case Text("Close", _) => Close()
          case Text("Error", _) => throw new Exception("TestWebsocketServer introduced error")
          case other => other
        }

      Queue.unbounded[IO, WebSocketFrame].flatMap { q =>
        val d = q.dequeue.through(echoReply)
        val e = q.enqueue
        WebSocketBuilder[IO].build(d, e)
      }


    case GET -> Root / aString =>
      Ok(aString)
  }

  def start(port: Int): Fiber[IO, ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(port)
      .withWebSockets(true)
      .withHttpApp(routes.orNotFound)
      .resource.use[ExitCode](_ => IO.never).start.unsafeRunSync()

}
