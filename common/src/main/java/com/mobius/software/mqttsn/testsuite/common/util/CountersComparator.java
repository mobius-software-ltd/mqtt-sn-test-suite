package com.mobius.software.mqttsn.testsuite.common.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.mobius.software.mqttsn.parser.avps.SNType;
import com.mobius.software.mqttsn.testsuite.common.model.Counters;

public class CountersComparator implements Comparator<Counters>
{
	private static final Map<SNType, Integer> counterWeight = new HashMap<>();
	static
	{
		counterWeight.put(SNType.CONNECT, 1);
		counterWeight.put(SNType.CONNACK, 2);
		counterWeight.put(SNType.WILL_MSG_REQ, 3);
		counterWeight.put(SNType.WILL_MSG, 4);
		counterWeight.put(SNType.WILL_TOPIC_REQ, 5);
		counterWeight.put(SNType.WILL_TOPIC, 6);
		counterWeight.put(SNType.WILL_MSG_UPD, 7);
		counterWeight.put(SNType.WILL_MSG_RESP, 8);
		counterWeight.put(SNType.WILL_TOPIC_UPD, 9);
		counterWeight.put(SNType.WILL_TOPIC_RESP, 10);
		counterWeight.put(SNType.REGISTER, 11);
		counterWeight.put(SNType.REGACK, 12);
		counterWeight.put(SNType.SUBSCRIBE, 13);
		counterWeight.put(SNType.SUBACK, 14);
		counterWeight.put(SNType.PUBLISH, 15);
		counterWeight.put(SNType.PUBACK, 16);
		counterWeight.put(SNType.PUBREC, 17);
		counterWeight.put(SNType.PUBREL, 16);
		counterWeight.put(SNType.PUBCOMP, 17);
		counterWeight.put(SNType.UNSUBSCRIBE, 18);
		counterWeight.put(SNType.UNSUBACK, 19);
		counterWeight.put(SNType.PINGREQ, 20);
		counterWeight.put(SNType.PINGRESP, 21);
		counterWeight.put(SNType.DISCONNECT, 22);

	}

	@Override
	public int compare(Counters o1, Counters o2)
	{
		SNType type2 = o2.getIn().getCommand();
		if (type2 == null)
			return -1;
		SNType type1 = o1.getIn().getCommand();
		if (type1 == null)
			return 1;
		Integer weight2 = counterWeight.get(type2);
		if (weight2 == null)
			return -1;
		Integer weight1 = counterWeight.get(type1);
		if (weight1 == null)
			return 1;
		return Integer.compare(weight1, weight2);
	}
}
