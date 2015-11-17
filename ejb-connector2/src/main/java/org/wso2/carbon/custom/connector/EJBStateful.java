/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.custom.connector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.lang.reflect.Method;

public class EJBStateful extends AbstractConnector {
    private static final Log log = LogFactory.getLog(EJBStateful.class);
    EJBUtil ejbUtil = new EJBUtil();
    Method method;
    Object ejbObj = null;

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        Thread currentThread = Thread.currentThread();
        ClassLoader oldClassLoader = currentThread.getContextClassLoader();
        try {
            //switching the classloader to prevent class loading glassfish classloading issues
            currentThread.setContextClassLoader(getClass().getClassLoader());
            callEJBStateful(messageContext);
        } catch (Exception e) {
            handleException("Error calling EJB Service from EJBConnector", e, messageContext);
        } finally {
            if (oldClassLoader != null) {
                //resetting the classloader
                currentThread.setContextClassLoader(oldClassLoader);
            }
        }
    }

    /**
     * Calls the EJB Service..
     * @param messageContext
     * @throws Exception
     */
    public void callEJBStateful(MessageContext messageContext) throws Exception {
        String methodName = (String) getParameter(messageContext, EJBConstance.METHOD_NAME);
        String returnName = (String) getParameter(messageContext, EJBConstance.RETURN);
        if (getParameter(messageContext, EJBConstance.RETURN) == null) {
            returnName = EJBConstance.RESPONSE;
        }
        log.info("method " + methodName + " Initializing");
        if (messageContext.getProperty(messageContext.getMessageID()) == null) {
            ejbObj = ejbUtil.getEJBObject(messageContext, EJBConstance.JNDI_NAME);
            messageContext.setProperty(messageContext.getMessageID(), ejbObj);
        } else {
            ejbObj = messageContext.getProperty(messageContext.getMessageID());
        }
        Class aClass = ejbObj.getClass();
        Object[] args = ejbUtil.buildArguments(messageContext, EJBConstance.STATEFUL);
        try {
            method = ejbUtil.resolveMethod(Class.forName(aClass.getName()),
                    methodName, args.length, messageContext);
            Object obj = ejbUtil.invokeInstanceMethod(ejbObj, method, args, messageContext);
            if (!method.getReturnType().toString().equals(EJBConstance.VOID)) {
                messageContext.setProperty(returnName, obj);
            } else {
                messageContext.setProperty(EJBConstance.RESPONSE, EJBConstance.SUCCESS);
            }
        } catch (ClassNotFoundException e) {
            handleException("Could not load '" + aClass.getName() + "' class.", e, messageContext);
        }
    }
}