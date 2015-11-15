/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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

import javax.xml.stream.XMLStreamException;
import java.lang.reflect.Method;

public class EJBStatelessBean extends AbstractConnector {

    private static final Log log = LogFactory.getLog(EJBStatelessBean.class);
    EJBFactory ejbFacrory = new EJBFactory();
    Method method;
    @Override
    public void connect(MessageContext ctx) {
        log.info("Initializing EJBConnector");
        Thread currentThread = Thread.currentThread();
        ClassLoader oldClassLoader = currentThread.getContextClassLoader();
        try {
            //switching the classloader to prevent class loading glassfish classloading issues
            currentThread.setContextClassLoader(getClass().getClassLoader());
            callEJB(ctx);
        } catch (Exception e) {
            handleException("Error calling EJB Service from EJBConnector", e, ctx);
        } finally {
            if (oldClassLoader != null) {
                //resetting the classloader
                currentThread.setContextClassLoader(oldClassLoader);
            }
        }
    }


    /**
     * Calls the EJB Service..
     *
     * @param ctx
     * @throws Exception
     */
    private void callEJB(MessageContext ctx) {
        Object ejbObj = ejbFacrory.getEJBObject(ctx,EJBConstance.JNDI_NAME);
        Class aClass = ejbObj.getClass();
        Object[] args = ejbFacrory.buildArguments(ctx, EJBConstance.STATELESS);
        String methodName = getParameter(ctx, EJBConstance.METHOD_NAME).toString();
        try {
            method = ejbFacrory.resolveMethod(Class.forName(aClass.getName()),
                    methodName, args.length);
            ctx.setProperty(EJBConstance.Response, ejbFacrory.invokeInstanceMethod(ejbObj, method, args));
        } catch (ClassNotFoundException e) {
            handleException("Could not load '" + aClass.getName() + "' class.", e, ctx);
        } catch (XMLStreamException e) {
            handleException("Could not load '" + aClass.getName() + "' class.", e, ctx);
        }
    }
}
