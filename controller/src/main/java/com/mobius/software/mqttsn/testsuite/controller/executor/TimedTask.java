package com.mobius.software.mqttsn.testsuite.controller.executor;

import java.util.concurrent.Delayed;

public interface TimedTask extends Delayed
{
	Boolean execute();

	Long getRealTimestamp();

	void stop();
}