package com.mobius.software.mqttsn.testsuite.common.rest.annotations;

public @interface Required
{
	RequiredType type() default RequiredType.NOT_NULL;
}
