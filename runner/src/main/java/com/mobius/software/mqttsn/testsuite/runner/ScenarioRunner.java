package com.mobius.software.mqttsn.testsuite.runner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobius.software.mqttsn.testsuite.common.model.ClientError;
import com.mobius.software.mqttsn.testsuite.common.model.ScenarioContext;
import com.mobius.software.mqttsn.testsuite.common.model.ScenarioReport;
import com.mobius.software.mqttsn.testsuite.common.model.ScenarioRequest;
import com.mobius.software.mqttsn.testsuite.common.rest.ControllerRequest;
import com.mobius.software.mqttsn.testsuite.common.util.FileUtil;
import com.mobius.software.mqttsn.testsuite.common.util.ReportBuilder;
import com.mobius.software.mqttsn.testsuite.runner.executor.RequestExecutor;
import com.mobius.software.mqttsn.testsuite.runner.util.TemplateParser;

public class ScenarioRunner
{
	private ControllerRequest requestData;
	private RequestExecutor requestExecutor = new RequestExecutor();

	public ScenarioRunner(ControllerRequest requestData)
	{
		this.requestData = requestData;
	}

	public static void main(String[] args)
	{
		try
		{
			Args arguments = Args.parse(args);
			ControllerRequest requestData = parseRequests(new File(arguments.getRequestFilepath()).getAbsolutePath());

			ScenarioRunner runner = new ScenarioRunner(requestData);
			runner.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	//TODO: move to configuration
	public static ControllerRequest parseRequests(String absoluteFilePath) throws IllegalArgumentException, URISyntaxException, JsonParseException, JsonMappingException, IOException
	{
		File json = FileUtil.readFile(absoluteFilePath);
		TemplateParser tempateParser = new TemplateParser();
		tempateParser.addTemplate("controller.1.ip", "13.93.104.25");
		tempateParser.addTemplate("controller.1.port", "9998");
		tempateParser.addTemplate("controller.2.ip", "51.144.103.59");
		tempateParser.addTemplate("controller.2.port", "9998");
		//tempateParser.addTemplate("controller.3.ip", "");
		//tempateParser.addTemplate("controller.3.port", "9998");
		tempateParser.addTemplate("broker.ip", "13.94.158.185");
		
		String textual = tempateParser.fileToStringProcessTemplates(json);
		ControllerRequest data = new ObjectMapper().readValue(textual, ControllerRequest.class);
		if (!data.validate())
			throw new IllegalArgumentException("JSON file: one of the required fields is missing or invalid");

		return data;
	}

	public void start() throws InterruptedException, ExecutionException
	{
		System.out.println("sending requests...");
		List<ScenarioContext> scenarioContexts = sendScenarioRequests();
		System.out.println("awaiting " + requestData.getRequestTimeout() + "ms");
		Thread.sleep(requestData.getRequestTimeout());
		List<ScenarioReport> reports = fetchReportData(scenarioContexts);
		for (ScenarioReport report : reports)
		{
			for (ScenarioRequest request : requestData.getData())
			{
				if (report.getScenarioID().equals(request.getScenario().getId()))
				{
					String summary = ReportBuilder.buildSummary(report);
					if (!summary.isEmpty())
						System.out.println(summary);

					String errorsText = getErrorsAsString(report.getErrors());
					if (!errorsText.isEmpty())
						System.out.println(errorsText);

					break;
				}
			}
		}
	}

	private String getErrorsAsString(List<ClientError> errors)
	{
		StringBuilder sb = new StringBuilder();
		if (errors != null)
		{
			for (ClientError error : errors)
				sb.append(error.toString()).append("\n");

		}
		return sb.toString();
	}

	private List<ScenarioContext> sendScenarioRequests() throws InterruptedException, ExecutionException
	{
		return requestExecutor.execute(requestData).get();
	}

	private List<ScenarioReport> fetchReportData(List<ScenarioContext> ctx) throws InterruptedException, ExecutionException
	{
		if (ctx == null || ctx.isEmpty())
			return Collections.emptyList();

		return requestExecutor.fetchReport(ctx).get();
	}
}
