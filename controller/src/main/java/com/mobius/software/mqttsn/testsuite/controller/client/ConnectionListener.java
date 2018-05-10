package com.mobius.software.mqttsn.testsuite.controller.client;

import com.mobius.software.mqttsn.parser.packet.api.SNMessage;

public interface ConnectionListener
{
	void packetReceived(SNMessage header);
}
