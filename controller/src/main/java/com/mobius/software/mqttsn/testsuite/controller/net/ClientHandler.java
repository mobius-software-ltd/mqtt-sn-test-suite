package com.mobius.software.mqttsn.testsuite.controller.net;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mobius.software.mqttsn.parser.Parser;
import com.mobius.software.mqttsn.parser.packet.api.SNMessage;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

@Sharable
public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket>
{
	private static final Log logger = LogFactory.getLog(ClientHandler.class);

	private ConcurrentHashMap<SocketAddress, ClientSession> sessions;

	public ClientHandler(ConcurrentHashMap<SocketAddress, ClientSession> sessions)
	{
		this.sessions = sessions;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet)
	{
		SocketAddress clientAddress = ctx.channel().localAddress();
		ClientSession session = sessions.get(clientAddress);
		if (session != null)
		{
			try
			{
				SNMessage message = Parser.decode(packet.content());
				session.getListener().packetReceived(message);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				logger.error("CLIENT HANDER: ERROR OCCURED!");
			}

		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx)
	{
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		cause.printStackTrace();
	}
}
