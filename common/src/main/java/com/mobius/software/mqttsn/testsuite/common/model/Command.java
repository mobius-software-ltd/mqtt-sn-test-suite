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

package com.mobius.software.mqttsn.testsuite.common.model;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.mobius.software.mqttsn.parser.avps.SNType;
import com.mobius.software.mqttsn.testsuite.common.util.CommandParser;

public class Command
{
	private SNType type;
	private Long sendTime;
	private List<Property> commandProperties;

	@Override
	public String toString()
	{
		return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
	}

	public Command()
	{

	}

	public Command(SNType type, Long sendTime, List<Property> commandProperties)
	{
		this.type = type;
		this.sendTime = sendTime;
		this.commandProperties = commandProperties;
	}

	public SNType getType()
	{
		return type;
	}

	public void setType(SNType type)
	{
		this.type = type;
	}

	public Long getSendTime()
	{
		return sendTime;
	}

	public void setSendTime(Long sendTime)
	{
		this.sendTime = sendTime;
	}

	public List<Property> getCommandProperties()
	{
		return commandProperties != null ? commandProperties : Collections.<Property> emptyList();
	}

	public void setCommandProperties(List<Property> commandProperties)
	{
		this.commandProperties = commandProperties;
	}

	public boolean validate()
	{
		return type != null && sendTime != null && CommandParser.validate(this);
	}
}
