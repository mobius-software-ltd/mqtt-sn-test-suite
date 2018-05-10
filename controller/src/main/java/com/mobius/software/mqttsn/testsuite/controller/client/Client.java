package com.mobius.software.mqttsn.testsuite.controller.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.mobius.software.mqttsn.parser.avps.FullTopic;
import com.mobius.software.mqttsn.parser.avps.IdentifierTopic;
import com.mobius.software.mqttsn.parser.avps.ReturnCode;
import com.mobius.software.mqttsn.parser.avps.SNQoS;
import com.mobius.software.mqttsn.parser.avps.SNType;
import com.mobius.software.mqttsn.parser.packet.api.CountableMessage;
import com.mobius.software.mqttsn.parser.packet.api.SNMessage;
import com.mobius.software.mqttsn.parser.packet.impl.*;
import com.mobius.software.mqttsn.testsuite.common.model.Command;
import com.mobius.software.mqttsn.testsuite.common.model.ErrorType;
import com.mobius.software.mqttsn.testsuite.common.model.ReportData;
import com.mobius.software.mqttsn.testsuite.common.util.CommandParser;
import com.mobius.software.mqttsn.testsuite.controller.Orchestrator;
import com.mobius.software.mqttsn.testsuite.controller.executor.TaskExecutor;
import com.mobius.software.mqttsn.testsuite.controller.executor.TimedTask;
import com.mobius.software.mqttsn.testsuite.controller.net.NetworkHandlerException;
import com.mobius.software.mqttsn.testsuite.controller.net.UDPClient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Client implements TimedTask, ConnectionListener
{
	private final String clientID;
	private final InetSocketAddress serverAddress;
	private final String localHostname;
	private final ConcurrentLinkedQueue<Command> commands;
	private final Orchestrator orchestrator;
	private boolean continueOnError;

	private ClientContext ctx = new ClientContext();
	private ReportData report;
	private AtomicLong timestamp;

	private AtomicReference<Command> pendingCommand = new AtomicReference<>();

	private ResendTimer pingTimer;

	private AtomicBoolean isConnected = new AtomicBoolean(false);
	private AtomicBoolean terminated = new AtomicBoolean();

	private Client(String clientID, InetSocketAddress serverAddress, String localHostname, ConcurrentLinkedQueue<Command> commands, Orchestrator orchestrator, boolean continueOnError)
	{
		this.clientID = clientID;
		this.serverAddress = serverAddress;
		this.localHostname = localHostname;
		this.commands = commands;
		this.report = new ReportData(clientID, commands.size());
		this.orchestrator = orchestrator;
		this.continueOnError = continueOnError;
		this.timestamp = new AtomicLong(System.currentTimeMillis() + orchestrator.getProperties().getInitialDelay() + orchestrator.getProperties().getScenarioDelay());
	}

	@Override
	public Boolean execute()
	{
		if (terminated.get())
			return false;

		Command previousCommand = pendingCommand.getAndSet(null);
		if (previousCommand != null)
		{
			report.error(ErrorType.PREVIOUS_COMMAND_FAILED, "previous failed:" + previousCommand.getType() + ", clientAddress:" + ctx.getLocalAddress());
			if (!continueOnError)
			{
				terminated.set(true);
				return false;
			}
		}

		boolean doAwaitResponse = true;
		Command currCommand = commands.poll();
		if (currCommand != null)
		{
			try
			{
				SNMessage message = CommandParser.toMessage(currCommand, clientID);
				if (message.getType() != SNType.CONNECT && message.getType() != SNType.DISCONNECT && !isConnected.get())
				{
					pendingCommand.set(null);
					return doAwaitResponse;
				}

				switch (message.getType())
				{
				case CONNECT:
					SocketAddress clientAddress = UDPClient.getInstance().createSession(serverAddress, localHostname, this);
					ctx.localAddress(clientAddress);
					SNConnect connect = (SNConnect) message;
					ctx.setDuration(connect.getDuration());
					pendingCommand.set(currCommand);
					break;
				case WILL_MSG:
					WillMsg willMsg = (WillMsg) message;
					updateWillMessageContext(willMsg.getContent());
					break;
				case WILL_TOPIC:
					WillTopic willTopic = (WillTopic) message;
					updateWillTopic(willTopic.getTopic(), willTopic.isRetain());
					break;
				case WILL_MSG_UPD:
					WillMsgUpd willMsgUpd = (WillMsgUpd) message;
					updateWillMessageContext(willMsgUpd.getContent());
					break;
				case WILL_TOPIC_UPD:
					WillTopicUpd willTopicUpdate = (WillTopicUpd) message;
					updateWillTopic(willTopicUpdate.getTopic(), willTopicUpdate.isRetain());
					break;
				case SUBSCRIBE:
					SNSubscribe subscribe = (SNSubscribe) message;
					subscribe.setMessageID(ctx.updateOutgoingPacketID(message.getType()));
					pendingCommand.set(currCommand);
					break;
				case REGISTER:
					Register register = (Register) message;
					register.setMessageID(ctx.updateOutgoingPacketID(message.getType()));
					pendingCommand.set(currCommand);
					break;
				case PUBLISH:
					SNPublish publish = (SNPublish) message;
					if (publish.getTopic().getQos() != SNQoS.AT_MOST_ONCE)
					{
						publish.setMessageID(ctx.updateOutgoingPacketID(message.getType()));
						pendingCommand.set(currCommand);
					}
					else
						doAwaitResponse = false;
					FullTopic currPublishTopic = (FullTopic) publish.getTopic();
					Integer publishTopicCode = ctx.getTopicCode(currPublishTopic.getValue());
					if (publishTopicCode == null)
						throw new IllegalArgumentException("publish topic is not registered");

					IdentifierTopic publishTopic = new IdentifierTopic(publishTopicCode, currPublishTopic.getQos());
					publish.setTopic(publishTopic);
					break;
				case UNSUBSCRIBE:
					SNUnsubscribe unsubscribe = (SNUnsubscribe) message;
					unsubscribe.setMessageID(ctx.updateOutgoingPacketID(message.getType()));
					pendingCommand.set(currCommand);
					break;
				case PINGREQ:
					break;
				case DISCONNECT:
					stopPingTimer();
					isConnected.set(false);
					pendingCommand.set(currCommand);
					break;

				case CONNACK:
				case SUBACK:
				case PINGRESP:
				case UNSUBACK:
				case REGACK:
				case WILL_MSG_REQ:
				case WILL_MSG_RESP:
				case WILL_TOPIC_REQ:
				case WILL_TOPIC_RESP:
				case PUBACK:
				case PUBREC:
				case PUBREL:
				case PUBCOMP:
					throw new IllegalArgumentException("invalid command " + message.getType());

				case ADVERTISE:
				case ENCAPSULATED:
				case GWINFO:
				case SEARCHGW:
				default:
					throw new IllegalArgumentException("unsupported command " + message.getType());
				}

				sendMessage(message);
			}
			catch (Exception e)
			{
				report.error(ErrorType.MESSAGE_SEND, "an error occured while sending " + currCommand.getType() + ":" + e.getMessage() + ". clientAddress:" + ctx.getLocalAddress());
				if (!continueOnError)
					return false;
			}
		}

		setNextTimestamp();
		if (!doAwaitResponse)
		{
			report.countFinishedCommand();
			orchestrator.notifyOnComplete(this);
		}

		return !commands.isEmpty();
	}

	private void setNextTimestamp()
	{
		Command next = commands.peek();
		if (next != null)
			timestamp.set(System.currentTimeMillis() + next.getSendTime());
		else
			terminated.set(true);
	}

	private void updateWillMessageContext(ByteBuf content)
	{
		byte[] bytes = new byte[content.readableBytes()];
		content.readBytes(bytes);
		ctx.willMessage(bytes);
	}

	private void updateWillTopic(FullTopic topic, boolean retain)
	{
		ctx.willTopic(topic.getValue(), topic.getQos(), retain);
	}

	@Override
	public void packetReceived(SNMessage message)
	{
		report.countIn(message.getType());
		try
		{
			processReceived(message);
		}
		catch (NetworkHandlerException e)
		{
			report.error(ErrorType.MESSAGE_SEND, "an error occure while sending message " + message.getType() + "," + e.getMessage());
		}

		if (message.getType() != SNType.PINGRESP)
			pendingCommand.set(null);
	}

	private void processReceived(SNMessage message) throws NetworkHandlerException
	{
		switch (message.getType())
		{
		case CONNACK:
			pendingCommand.set(null);
			SNConnack connack = (SNConnack) message;
			if (connack.getCode() != ReturnCode.ACCEPTED)
				report.error(ErrorType.MESSAGE_RECEIVE, message.getType() + " returned error code " + connack.getCode());
			else
			{
				isConnected.set(true);
				if (ctx.getDuration() > 0)
				{
					pingTimer = new ResendTimer(new SNPingreq(), ctx.getLocalAddress(), ctx.getDuration() * 1000, report);
					TaskExecutor.getInstance().schedule(pingTimer);
				}
				report.countFinishedCommand();
				orchestrator.notifyOnStart(this);
			}
			break;
		case WILL_MSG_REQ:
			WillMsg willMsg = new WillMsg(Unpooled.copiedBuffer(ctx.getWillMessage()));
			sendMessage(willMsg);
			break;
		case WILL_TOPIC_REQ:
			FullTopic willTopicValue = new FullTopic(ctx.getWillTopic(), ctx.getWillQos());
			WillTopic willTopic = new WillTopic(ctx.getWillRetain(), willTopicValue);
			sendMessage(willTopic);
			break;
		case REGISTER:
			Register register = (Register) message;
			ctx.storeTopic(register.getTopicID(), register.getTopicName());
			Regack registerAck = new Regack(register.getTopicID(), register.getMessageID(), ReturnCode.ACCEPTED);
			sendMessage(registerAck);
			break;
		case REGACK:
			Regack regack = (Regack) message;
			processAckMessage(regack, SNType.REGISTER);
			if (regack.getCode() != ReturnCode.ACCEPTED)
				report.error(ErrorType.MESSAGE_RECEIVE, message.getType() + " returned error code " + regack.getCode());
			else
			{
				Command pendingRegisterCommand = pendingCommand.getAndSet(null);
				if (pendingRegisterCommand == null)
				{
					report.error(ErrorType.MESSAGE_RECEIVE, "received unexpected regack");
				}
				else
				{
					SNMessage mustBeRegister = CommandParser.toMessage(pendingRegisterCommand, clientID);
					if (mustBeRegister.getType() != SNType.REGISTER)
						report.error(ErrorType.MESSAGE_RECEIVE, "received unexpected regack. Pending message " + mustBeRegister.getType());
					else
					{
						Register pendingRegister = (Register) mustBeRegister;
						ctx.storeTopic(regack.getTopicID(), pendingRegister.getTopicName());
						report.countFinishedCommand();
						orchestrator.notifyOnComplete(this);
					}
				}
			}
			break;
		case SUBACK:
			Command pendingSubscribeCommand = pendingCommand.getAndSet(null);
			if (pendingSubscribeCommand == null)
				report.error(ErrorType.MESSAGE_RECEIVE, "received unexpected suback");
			else
			{
				SNMessage mustBeSubscribe = CommandParser.toMessage(pendingSubscribeCommand, clientID);
				if (mustBeSubscribe.getType() != SNType.SUBSCRIBE)
					report.error(ErrorType.MESSAGE_RECEIVE, "received unexpected suback. Pending message " + mustBeSubscribe.getType());
				else
				{
					SNSubscribe pendingSubscribe = (SNSubscribe) mustBeSubscribe;
					SNSuback suback = (SNSuback) message;
					processAckMessage(suback, SNType.SUBSCRIBE);
					if (suback.getCode() != ReturnCode.ACCEPTED)
						report.error(ErrorType.MESSAGE_RECEIVE, message.getType() + " returned error code " + suback.getCode());
					else
					{
						String topic = ((FullTopic) pendingSubscribe.getTopic()).getValue();
						ctx.storeTopic(suback.getTopicID(), topic);
						report.countFinishedCommand();
						orchestrator.notifyOnComplete(this);
					}
				}
			}
			break;
		case PUBLISH:
			SNPublish publish = (SNPublish) message;
			if (publish.isDup())
				report.countDuplicateIn();
			switch (publish.getTopic().getQos())
			{
			case AT_LEAST_ONCE:
				pendingCommand.set(null);
				IdentifierTopic publishTopic = (IdentifierTopic) publish.getTopic();
				SNPuback publishAck = new SNPuback(publishTopic.getValue(), publish.getMessageID(), ReturnCode.ACCEPTED);
				sendMessage(publishAck);
				break;
			case EXACTLY_ONCE:
				pendingCommand.set(null);
				SNPubrec publishRec = new SNPubrec(publish.getMessageID());
				sendMessage(publishRec);
				break;
			case LEVEL_ONE:
			case AT_MOST_ONCE:
			default:
				break;
			}
			break;
		case PUBACK:
			SNPuback puback = (SNPuback) message;
			if (processAckMessage(puback, SNType.PUBLISH))
			{
				pendingCommand.set(null);
				report.countFinishedCommand();
				orchestrator.notifyOnComplete(this);
			}
			break;
		case PUBREC:
			SNPubrec pubrec = (SNPubrec) message;
			if (processAckMessage(pubrec, SNType.PUBLISH))
			{
				SNPubrel pubrecAck = new SNPubrel(pubrec.getMessageID());
				sendMessage(pubrecAck);
			}
			break;
		case PUBCOMP:
			SNPubcomp pubcomp = (SNPubcomp) message;
			if (processAckMessage(pubcomp, SNType.PUBREL))
			{
				pendingCommand.set(null);
				report.countFinishedCommand();
				orchestrator.notifyOnComplete(this);
			}
			break;
		case PUBREL:
			SNPubrel pubrel = (SNPubrel) message;
			SNPubcomp pubrelAck = new SNPubcomp(pubrel.getMessageID());
			sendMessage(pubrelAck);
			break;
		case UNSUBACK:
			SNUnsuback unsuback = (SNUnsuback) message;
			if (processAckMessage(unsuback, SNType.UNSUBSCRIBE))
			{
				pendingCommand.set(null);
				report.countFinishedCommand();
				orchestrator.notifyOnComplete(this);
			}
			break;
		case PINGRESP:
			break;
		case DISCONNECT:
			stopPingTimer();
			UDPClient.getInstance().stopSession(ctx.getLocalAddress());
			Command mustBeDisconnect = pendingCommand.get();
			if (mustBeDisconnect != null && mustBeDisconnect.getType() == SNType.DISCONNECT)
			{
				pendingCommand.set(null);
				report.countFinishedCommand();
				orchestrator.notifyOnComplete(this);
			}
			break;
		case ADVERTISE:
		case ENCAPSULATED:
		case GWINFO:
		case SEARCHGW:
			report.error(ErrorType.MESSAGE_RECEIVE, "received unsupported message from server " + message.getType());
			break;

		case CONNECT:
		case PINGREQ:
		case SUBSCRIBE:
		case UNSUBSCRIBE:
		case WILL_MSG:
		case WILL_MSG_RESP:
		case WILL_MSG_UPD:
		case WILL_TOPIC:
		case WILL_TOPIC_RESP:
		case WILL_TOPIC_UPD:
		default:
			report.error(ErrorType.MESSAGE_RECEIVE, "received invalid message from server " + message.getType());
			break;
		}
	}

	private boolean processAckMessage(CountableMessage incomingCountable, SNType expected)
	{
		SNType actual = null;
		if (incomingCountable instanceof SNPubrec)
			actual = ctx.changeOutgoingPacketID(incomingCountable.getMessageID(), SNType.PUBREL);
		else
			actual = ctx.releaseOutgoingPacketID(incomingCountable.getMessageID());

		boolean result = processAck(incomingCountable.getType(), actual, expected);
		if (!result)
			ctx.releaseOutgoingPacketID(incomingCountable.getMessageID());
		return result;
	}

	private boolean processAck(SNType receivedType, SNType actual, SNType expected)
	{
		boolean isSuccess = true;
		if (actual == null)
		{
			isSuccess = false;
			report.error(ErrorType.MESSAGE_RECEIVE, "Received unexpected " + receivedType + ". messageID not registered");
		}
		else if (actual != expected)
		{
			isSuccess = false;
			report.error(ErrorType.MESSAGE_RECEIVE, "Received unexpected " + receivedType + ". messageID corresponds to " + actual + " when expected " + expected);
		}
		return isSuccess;
	}

	private void sendMessage(SNMessage message) throws NetworkHandlerException
	{
		UDPClient.getInstance().send(ctx.getLocalAddress(), message);
		report.countOut(message.getType());
	}

	@Override
	public Long getRealTimestamp()
	{
		return timestamp.get();
	}

	@Override
	public void stop()
	{
		if (!terminated.compareAndSet(false, true))
			return;

		stopPingTimer();
		UDPClient.getInstance().stopSession(ctx.getLocalAddress());
	}

	private void stopPingTimer()
	{
		if (pingTimer != null)
		{
			pingTimer.stop();
			pingTimer = null;
		}
	}

	public ReportData getReport()
	{
		return report;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public boolean hasMoreCommands()
	{
		return !commands.isEmpty();
	}

	public static class Builder
	{
		private String clientID;
		private InetSocketAddress serverAddress;
		private String localHostname;
		private ConcurrentLinkedQueue<Command> commands;
		private Orchestrator orchestrator;
		private boolean continueOnError;

		public Builder clientID(String clientID)
		{
			this.clientID = clientID;
			return this;
		}

		public Builder serverAddress(InetSocketAddress serverAddress)
		{
			this.serverAddress = serverAddress;
			return this;
		}

		public Builder commands(ConcurrentLinkedQueue<Command> commands)
		{
			this.commands = commands;
			return this;
		}

		public Builder orchestrator(Orchestrator orchestrator)
		{
			this.orchestrator = orchestrator;
			return this;
		}

		public Builder continueOnError(boolean continueOnError)
		{
			this.continueOnError = continueOnError;
			return this;
		}

		public Builder localHostname(String localHostname)
		{
			this.localHostname = localHostname;
			return this;
		}

		public Client build()
		{
			return new Client(clientID, serverAddress, localHostname, commands, orchestrator, continueOnError);
		}
	}

	@Override
	public long getDelay(TimeUnit unit)
	{
		long diff = timestamp.get() - System.currentTimeMillis();
		return unit.convert(diff, TimeUnit.MILLISECONDS);
	}

	@Override
	public int compareTo(Delayed o)
	{
		if (o == null)
			return 1;
		if (o == this)
			return 0;
		long diff = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
		return diff > 0 ? 1 : diff == 0 ? 0 : -1;
	}
}
