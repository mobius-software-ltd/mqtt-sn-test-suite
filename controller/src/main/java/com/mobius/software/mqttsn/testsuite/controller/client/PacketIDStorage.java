package com.mobius.software.mqttsn.testsuite.controller.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mobius.software.mqttsn.parser.avps.SNType;

public class PacketIDStorage
{
	private static final int MAX_VALUE = 65535;
	private static final int FIRST_ID = 1;

	private ConcurrentHashMap<Integer, SNType> pendingMessages = new ConcurrentHashMap<>();
	private AtomicInteger currID = new AtomicInteger();

	public int updateAndStoreIDFor(SNType msg)
	{
		Integer messageID = null;
		do
		{
			if (pendingMessages.size() == MAX_VALUE)
				throw new IllegalStateException("OUTGOING IDENTIFIER OVERFLOW");

			messageID = currID.incrementAndGet();
			if (currID.compareAndSet(MAX_VALUE, messageID))
				messageID = FIRST_ID;
		}
		while (pendingMessages.putIfAbsent(messageID, msg) != null);
		return messageID;
	}

	public SNType releaseID(int messageID)
	{
		return pendingMessages.remove(messageID);
	}

	public SNType changePacketID(int messageID, SNType newType)
	{
		return pendingMessages.put(messageID, newType);
	}
}
