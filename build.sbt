import scalajswebsocket.testsupport._

enablePlugins(ScalaJSPlugin)

name := "scalajs-websocket"

organization := "com.github.benhutchison"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.6",
  "co.fs2" %%% "fs2-core" % "1.0.0",
  "org.scalatest" %%% "scalatest" % "3.0.5" % "test",
)

jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()


testOptions in Test += Tests.Setup(() => TestWebsocketServer.start())

testOptions in Test += Tests.Cleanup(() => TestWebsocketServer.stop())

