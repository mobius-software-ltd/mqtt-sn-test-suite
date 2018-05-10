/**
 * Mobius Software LTD
 * Copyright 2015-2016, Mobius Software LTD
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.mobius.software.mqttsn.testsuite.common.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mobius.software.mqttsn.parser.Parser;
import com.mobius.software.mqttsn.parser.avps.FullTopic;
import com.mobius.software.mqttsn.parser.avps.SNQoS;
import com.mobius.software.mqttsn.parser.avps.SNTopic;
import com.mobius.software.mqttsn.parser.avps.SNType;
import com.mobius.software.mqttsn.parser.packet.api.SNMessage;
import com.mobius.software.mqttsn.parser.packet.impl.Register;
import com.mobius.software.mqttsn.parser.packet.impl.SNConnect;
import com.mobius.software.mqttsn.parser.packet.impl.SNPublish;
import com.mobius.software.mqttsn.parser.packet.impl.SNSubscribe;
import com.mobius.software.mqttsn.parser.packet.impl.SNUnsubscribe;
import com.mobius.software.mqttsn.testsuite.common.model.Command;
import com.mobius.software.mqttsn.testsuite.common.model.Property;
import com.mobius.software.mqttsn.testsuite.common.model.PropertyType;

import io.netty.buffer.ByteBuf;

public class CommandParser
{
	public static boolean validate(Command command)
	{
		if (command == null || command.getType() == null)
			return false;

		Map<PropertyType, String> propertyMap = new HashMap<>();
		if (command.getType() != SNType.DISCONNECT)
		{
			for (Property property : command.getCommandProperties())
				propertyMap.put(property.getType(), property.getValue());
		}
		try
		{
			switch (command.getType())
			{
			case CONNECT:

				String cleanSession = propertyMap.remove(PropertyType.CLEAN_SESSION);
				if (cleanSession == null || !(cleanSession.equals("true") || cleanSession.equals("false")))
					return false;
				String keepalive = propertyMap.remove(PropertyType.KEEPALIVE);
				if (keepalive == null || Integer.parseInt(keepalive) < 0)
					return false;
				return propertyMap.isEmpty();

			case DISCONNECT:
				return command.getCommandProperties() == null || command.getCommandProperties().isEmpty();

			case PUBLISH:
				String publishTopic = propertyMap.remove(PropertyType.TOPIC);
				if (publishTopic == null || publishTopic.isEmpty())
					return false;
				String publishQos = propertyMap.remove(PropertyType.QOS);
				if (publishQos == null || SNQoS.valueOf(Integer.parseInt(publishQos)) == null)
					return false;
				String retain = propertyMap.remove(PropertyType.RETAIN);
				if (retain != null && !(retain.equals("true") || retain.equals("false")))
					return false;
				String duplicate = propertyMap.remove(PropertyType.DUPLICATE);
				if (duplicate != null && !(duplicate.equals("true") || duplicate.equals("false")))
					return false;
				String count = propertyMap.remove(PropertyType.COUNT);
				if (count == null || Integer.parseInt(count) < 0)
					return false;
				String resendTime = propertyMap.remove(PropertyType.RESEND_TIME);
				if (resendTime == null || Integer.parseInt(resendTime) < 0)
					return false;
				String messageSize = propertyMap.remove(PropertyType.MESSAGE_SIZE);
				if (messageSize == null || Integer.parseInt(messageSize) < 0)
					return false;
				return propertyMap.isEmpty();

			case SUBSCRIBE:
				String subscribeTopic = propertyMap.remove(PropertyType.TOPIC);
				if (subscribeTopic == null || subscribeTopic.isEmpty())
					return false;
				String subscribeQos = propertyMap.remove(PropertyType.QOS);
				if (subscribeQos == null || SNQoS.valueOf(Integer.parseInt(subscribeQos)) == null)
					return false;
				return propertyMap.isEmpty();

			case UNSUBSCRIBE:
				String unsubscribeTopic = propertyMap.remove(PropertyType.TOPIC);
				if (unsubscribeTopic == null || unsubscribeTopic.isEmpty())
					return false;
				return propertyMap.isEmpty();

			case REGISTER:
				String registerTopic = propertyMap.remove(PropertyType.TOPIC);
				if (registerTopic == null || registerTopic.isEmpty())
					return false;
				return propertyMap.isEmpty();

			case WILL_MSG:
			case WILL_MSG_UPD:
				String willMsg = propertyMap.remove(PropertyType.WILL_MESSAGE);
				if (willMsg == null)
					return false;
				return propertyMap.isEmpty();

			case WILL_TOPIC:
			case WILL_TOPIC_UPD:
				String willTopic = propertyMap.remove(PropertyType.WILL_TOPIC);
				if (willTopic == null)
					return false;
				String willQos = propertyMap.remove(PropertyType.WILL_QOS);
				if (willQos == null || SNQoS.valueOf(Integer.parseInt(willQos)) == null)
					return false;
				String willRetain = propertyMap.remove(PropertyType.WILL_RETAIN);
				if (willRetain == null || !(willRetain.equals("true") || willRetain.equals("false")))
					return false;
				return propertyMap.isEmpty();

			default:
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public static SNMessage toMessage(Command command, String clientID)
	{
		Map<PropertyType, String> propertyMap = new HashMap<>();
		if (command.getType() != SNType.DISCONNECT)
			for (Property property : command.getCommandProperties())
				propertyMap.put(property.getType(), property.getValue());

		switch (command.getType())
		{
		case CONNECT:
			Boolean cleanSession = Boolean.parseBoolean(propertyMap.get(PropertyType.CLEAN_SESSION));
			Integer keepalive = Integer.parseInt(propertyMap.get(PropertyType.KEEPALIVE));
			return new SNConnect(cleanSession, keepalive, clientID, false);

		case DISCONNECT:
			return Parser.DISCONNECT;

		case PUBLISH:
			String publishTopicName = propertyMap.get(PropertyType.TOPIC);
			SNQoS publishQos = SNQoS.valueOf(Integer.valueOf(propertyMap.get(PropertyType.QOS)));
			SNTopic publishTopic = new FullTopic(publishTopicName, publishQos);
			Boolean retain = Boolean.parseBoolean(propertyMap.get(PropertyType.RETAIN));
			Boolean dup = Boolean.parseBoolean(propertyMap.get(PropertyType.DUPLICATE));
			int messageSize = Integer.parseInt(propertyMap.get(PropertyType.MESSAGE_SIZE));
			ByteBuf content = MessageGenerator.generateContent(messageSize);
			return new SNPublish(0, publishTopic, content, dup, retain);

		case SUBSCRIBE:
			String subscribeTopicName = propertyMap.get(PropertyType.TOPIC);
			SNQoS subscribeQos = SNQoS.valueOf(Integer.valueOf(propertyMap.get(PropertyType.QOS)));
			SNTopic subscribeTopic = new FullTopic(subscribeTopicName, subscribeQos);
			return new SNSubscribe(0, subscribeTopic, false);
			
		case UNSUBSCRIBE:
			String unsubscribeTopicname = propertyMap.get(PropertyType.TOPIC);
			SNTopic unsubscribeTopic = new FullTopic(unsubscribeTopicname, SNQoS.EXACTLY_ONCE);
			return new SNUnsubscribe(0, unsubscribeTopic);

		case REGISTER:
			String registerTopicname = propertyMap.get(PropertyType.TOPIC);
			return new Register(0, 0, registerTopicname);
		default:
			return null;
		}
	}

	public static ConcurrentLinkedQueue<Command> retrieveCommands(List<Command> commands, int repeatCount, long repeatInterval)
	{
		ConcurrentLinkedQueue<Command> queue = new ConcurrentLinkedQueue<>();
		long currInterval = 0L;
		while (repeatCount-- > 0)
		{
			for (int i = 0; i < commands.size(); i++)
			{
				Command command = commands.get(i);
				if (i == 0)
					command = new Command(command.getType(), command.getSendTime() + currInterval, command.getCommandProperties());
				queue.offer(command);
				if (command.getType() == SNType.PUBLISH)
				{
					long resendTime = retrieveIntegerProperty(command, PropertyType.RESEND_TIME);
					Integer count = retrieveIntegerProperty(command, PropertyType.COUNT);
					for (int j = 1; j < count; j++)
					{
						Command publish = new Command(command.getType(), resendTime, command.getCommandProperties());
						queue.offer(publish);
					}
				}
			}
			currInterval = repeatInterval;
		}
		return queue;
	}

	public static int count(Command command)
	{
		Integer count = retrieveIntegerProperty(command, PropertyType.COUNT);
		return count != null ? count : 0;
	}

	private static Integer retrieveIntegerProperty(Command command, PropertyType type)
	{
		Integer value = null;
		for (Property property : command.getCommandProperties())
		{
			if (property.getType() == type)
			{
				value = Integer.parseInt(property.getValue());
				break;
			}
		}
		return value;
	}
}
