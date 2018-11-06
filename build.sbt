import scalajswebsocket.testsupport._

enablePlugins(ScalaJSPlugin)

name := "scalajs-websocket"

organization := "com.github.benhutchison"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.6",
  "co.fs2" %%% "fs2-core" % "1.0.0",
  "org.specs2" %%% "specs2-core" % "4.3.5-78abffa2e-20181150936" % "test",
)

jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()


testOptions in Test += Tests.Setup(() => TestWebsocketServer.start())

testOptions in Test += Tests.Cleanup(() => TestWebsocketServer.stop())

