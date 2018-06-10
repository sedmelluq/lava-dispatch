package com.sedmelluq.lava.discord.dispatch;

import java.net.InetSocketAddress;

public class SocketAddressInfo {
  public final InetSocketAddress socketAddress;
  public final String hostAddress;
  public final int port;

  public SocketAddressInfo(InetSocketAddress socketAddress) {
    this.socketAddress = socketAddress;
    this.hostAddress = socketAddress.getAddress().getHostAddress();
    this.port = socketAddress.getPort();
  }
}
