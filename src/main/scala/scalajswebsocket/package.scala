
package object scalajswebsocket {

  // message op codes
  private[scalajswebsocket] val CONTINUATION = 0x0
  private[scalajswebsocket] val TEXT = 0x1
  private[scalajswebsocket] val BINARY = 0x2
  private[scalajswebsocket] val CLOSE = 0x8
  private[scalajswebsocket] val PING = 0x9
  private[scalajswebsocket] val PONG = 0xa

}
