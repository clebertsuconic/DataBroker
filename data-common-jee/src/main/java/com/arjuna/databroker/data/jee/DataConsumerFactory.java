/*
 * Copyright (c) 2013-2015, Arjuna Technologies Limited, Newcastle-upon-Tyne, England. All rights reserved.
 */

package com.arjuna.databroker.data.jee;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.arjuna.databroker.data.DataConsumer;
import com.arjuna.databroker.data.DataFlowNode;
import com.arjuna.databroker.data.connector.ObserverDataConsumer;
import com.arjuna.databroker.data.connector.ReferrerDataConsumer;

public class DataConsumerFactory
{
    private static final Logger logger = Logger.getLogger(DataConsumerFactory.class.getName());

    public static <T> DataConsumer<T> createDataConsumer(DataFlowNode dataFlowNode, String methodName, Class<T> dataClass)
    {
        logger.log(Level.FINE, "createDataConsumer");

        return new DefaultObserverDataConsumer<T>(dataFlowNode, methodName, dataClass);
    }

    public static <T> ObserverDataConsumer<T> createObserverDataConsumer(DataFlowNode dataFlowNode, String methodName, Class<T> dataClass)
    {
        logger.log(Level.FINE, "createObserverDataConsumer");

        return new DefaultObserverDataConsumer<T>(dataFlowNode, methodName, dataClass);
    }

    public static <T> ReferrerDataConsumer<T> createReferrerDataConsumer(DataFlowNode dataFlowNode, String methodName, Class<T> dataClass)
    {
        logger.log(Level.FINE, "createNamedDataProvider");

        return null;
    }
}
