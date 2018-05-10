package com.mobius.software.mqttsn.testsuite.common.tests;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobius.software.mqttsn.testsuite.common.rest.ControllerRequest;

public class UtilTests
{
	@Test
	public void testParseControllerRequest()
	{
		try
		{
			String resource = this.getClass().getClassLoader().getResource("json/scenario.json").getFile();
			File json = new File(resource);
			
			ObjectMapper mapper = new ObjectMapper();
			ControllerRequest data = mapper.readValue(json, ControllerRequest.class);
			System.out.println(data);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}
}
