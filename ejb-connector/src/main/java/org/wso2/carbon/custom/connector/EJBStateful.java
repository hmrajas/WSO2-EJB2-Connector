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

import javax.ejb.EJBHome;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EJBStateful extends AbstractConnector {
    private static final Log log = LogFactory.getLog(EJBStateful.class);
    EJBFactory ejbFacrory=new EJBFactory();
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        log.info("Initializing EJBConnector");
        Thread currentThread = Thread.currentThread();
        ClassLoader oldClassLoader = currentThread.getContextClassLoader();
        try {
            //switching the classloader to prevent class loading glassfish classloading issues
            currentThread.setContextClassLoader(getClass().getClassLoader());
            callEJB(messageContext);
        } catch (Exception e) {
            handleException("Error calling EJB Service from EJBConnector", e, messageContext);
        } finally {
            if (oldClassLoader != null) {
                //resetting the classloader
                currentThread.setContextClassLoader(oldClassLoader);
            }
        }
    }

    private void callEJB(MessageContext ctx) throws Exception {
        EJB2Init ejb2Init = new EJB2Init();
        InitialContext context;
        Object obj = null;
        try {
            context = ejb2Init.getInitialContext();
            obj = context.lookup(getParameter(ctx, EJBConstance.JNDI_NAME).toString());
        } catch (NoClassDefFoundError e) {
            handleException("Failed lookup because of ", ctx);
        } catch (NamingException e) {
            handleException("Failed lookup because of ", e, ctx);
        }
        EJBHome beanHome =
                (EJBHome) javax.rmi.PortableRemoteObject.narrow(obj, EJBHome.class);
        Method m = null;
        try {
            m = beanHome.getClass().getDeclaredMethod(EJBConstance.CREATE);
        } catch (NoSuchMethodException e) {
            handleException("There is no create method ", ctx);
        }
        Object ejbObj = null;
        try {
            if(m!=null){
                ejbObj = m.invoke(beanHome);
            }
            else {
                handleException("ejb home is missing",ctx);
            }
        } catch (IllegalAccessException e) {
            handleException("", e, ctx);
        } catch (InvocationTargetException e) {
            handleException("", e, ctx);
        }
        assert ejbObj != null;
        Class aClass = ejbObj.getClass();
        Object[] args = ejbFacrory.buildArguments(ctx, EJBConstance.STATEFUL);
        Method method;
        String methodName = getParameter(ctx, EJBConstance.METHOD_NAME).toString();
        try {
            method = ejbFacrory.resolveMethod(Class.forName(aClass.getName()),
                    methodName, args.length);
            ctx.setProperty(EJBConstance.Response, ejbFacrory.invokeInstanceMethod(ejbObj, method, args));
        } catch (ClassNotFoundException e) {
            handleException("Could not load '" + aClass.getName() + "' class.", e, ctx);
        }
    }


}
