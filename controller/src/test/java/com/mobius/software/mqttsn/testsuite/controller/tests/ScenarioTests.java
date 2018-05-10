package com.mobius.software.mqttsn.testsuite.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobius.software.mqttsn.testsuite.common.model.Scenario;
import com.mobius.software.mqttsn.testsuite.common.rest.ControllerRequest;
import com.mobius.software.mqttsn.testsuite.common.rest.GenericJsonObject;
import com.mobius.software.mqttsn.testsuite.common.rest.GenericRequest;
import com.mobius.software.mqttsn.testsuite.common.rest.GenericResponse;
import com.mobius.software.mqttsn.testsuite.common.rest.ResponseData;
import com.mobius.software.mqttsn.testsuite.controller.Controller;
import com.mobius.software.mqttsn.testsuite.controller.ControllerRunner;

public class ScenarioTests
{
	@BeforeClass
	public static void beforeClass()
	{
		try
		{
			String configFilePath = ScenarioTests.class.getClassLoader().getResource("config.properties").getPath();
			System.out.println("setting config file path: " + configFilePath);
			ControllerRunner.setConfigFilePath(configFilePath);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}

	@SuppressWarnings(
	{ "rawtypes", "unchecked" })
	@Test
	public void testConnect()
	{
		try
		{
			Scenario scenario = readScenarioFromResource("connect.json");

			GenericRequest<Scenario> request = new GenericRequest<Scenario>(scenario);

			Controller controller = new Controller();
			GenericJsonObject response = controller.scenario(request);
			assertNotNull(response);
			assertEquals("ERROR MSG: " + response.getMessage(), ResponseData.SUCCESS, response.getStatus());
			assertNull(response.getMessage());

			GenericResponse<UUID> objResponse = (GenericResponse) response;
			UUID scenarioID = objResponse.getData();
			assertNotNull(scenarioID);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}

	private Scenario readScenarioFromResource(String filename) throws JsonParseException, JsonMappingException, IOException
	{
		ClassLoader classLoader = getClass().getClassLoader();
		File json = new File(classLoader.getResource(filename).getFile());

		ControllerRequest data = new ObjectMapper().readValue(json, ControllerRequest.class);
		assertNotNull(data);
		assertNotNull(data.getData());
		assertEquals(1, data.getData().size());
		assertTrue("invalid scenario data", data.validate());

		return data.getData().get(0).getScenario();
	}
}
