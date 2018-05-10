package com.mobius.software.mqttsn.testsuite.controller.client;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import com.mobius.software.mqttsn.parser.avps.SNQoS;
import com.mobius.software.mqttsn.parser.avps.SNType;

public class ClientContext
{
	private boolean cleanSession;
	private SocketAddress localAddress;
	private byte[] willMessage;
	private String willTopic;
	private SNQoS willQos;
	private boolean willRetain;

	private int duration;

	private PacketIDStorage packetIDStorage = new PacketIDStorage();
	private ConcurrentHashMap<Integer, String> topics = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Integer> reverseTopics = new ConcurrentHashMap<>();

	public ClientContext cleanSession()
	{
		this.cleanSession = true;
		return this;
	}

	public ClientContext willMessage(byte[] msg)
	{
		this.willMessage = msg;
		return this;
	}

	public ClientContext willTopic(String topic, SNQoS qos, boolean retain)
	{
		this.willTopic = topic;
		this.willQos = qos;
		this.willRetain = retain;
		return this;
	}

	public String getTopicByCode(int code)
	{
		return topics.get(code);
	}

	public Integer getTopicCode(String topic)
	{
		return reverseTopics.get(topic);
	}

	public void storeTopic(int code, String topic)
	{
		String stored = topics.put(code, topic);
		if (stored == null || !stored.equals(topic))
			reverseTopics.put(topic, code);
	}

	public String removeTopic(int code)
	{
		String topic = topics.remove(code);
		if (topic != null)
			reverseTopics.remove(topic);
		return topic;
	}

	public int updateOutgoingPacketID(SNType type)
	{
		return packetIDStorage.updateAndStoreIDFor(type);
	}

	public SNType releaseOutgoingPacketID(int messageID)
	{
		return packetIDStorage.releaseID(messageID);
	}

	public SNType changeOutgoingPacketID(int messageID, SNType newType)
	{
		return packetIDStorage.changePacketID(messageID, newType);
	}

	public ClientContext localAddress(SocketAddress address)
	{
		this.localAddress = address;
		return this;
	}

	public boolean isCleanSession()
	{
		return cleanSession;
	}

	public SocketAddress getLocalAddress()
	{
		return localAddress;
	}

	public byte[] getWillMessage()
	{
		return willMessage;
	}

	public String getWillTopic()
	{
		return willTopic;
	}

	public SNQoS getWillQos()
	{
		return willQos;
	}

	public boolean getWillRetain()
	{
		return willRetain;
	}

	public int getDuration()
	{
		return duration;
	}

	public void setDuration(int duration)
	{
		this.duration = duration;
	}
}
