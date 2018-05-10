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

import java.util.HashMap;
import java.util.Map;

public enum PropertyType
{
	CLEAN_SESSION(3), KEEPALIVE(4), TOPIC(5), QOS(6), RETAIN(7), DUPLICATE(8), COUNT(9), RESEND_TIME(10), MESSAGE_SIZE(11), WILL_MESSAGE(12), WILL_TOPIC(13), WILL_RETAIN(14), WILL_QOS(15);

	private static final Map<Integer, PropertyType> intToTypeMap = new HashMap<Integer, PropertyType>();
	private static final Map<String, PropertyType> strToTypeMap = new HashMap<String, PropertyType>();

	static
	{
		for (PropertyType type : PropertyType.values())
		{
			intToTypeMap.put(type.value, type);
			strToTypeMap.put(type.name(), type);
		}
	}

	public static PropertyType fromInt(int i)
	{
		return intToTypeMap.get(Integer.valueOf(i));
	}

	int value;

	private PropertyType(int value)
	{
		this.value = value;
	}

	public int getValue()
	{
		return value;
	}

	public static PropertyType forValue(String value)
	{
		Integer intValue = null;
		try
		{
			intValue = Integer.parseInt(value);
		}
		catch (Exception ex)
		{

		}

		if (intValue != null)
			return intToTypeMap.get(intValue);
		else
			return strToTypeMap.get(value);
	}
}
