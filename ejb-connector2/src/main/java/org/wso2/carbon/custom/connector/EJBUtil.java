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

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.databinding.typemapping.SimpleTypeMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.Value;
import org.wso2.carbon.connector.core.util.ConnectorUtils;

import javax.ejb.EJBHome;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;


public class EJBUtil {
    EjbException ejbException = new EjbException();
    private final Log log = LogFactory.getLog(EJBUtil.class);


    /**
     * @param instance       instance of an ejb object
     * @param method         method that we want to call
     * @param messageContext messageContext
     * @param args           arguments object for method
     * @return invoke arguments into method and return the value which actual method returns
     */
    public Object invokeInstanceMethod(Object instance, Method method, Object[] args, MessageContext messageContext) {
        Class[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != args.length) {
            handleException("Provided argument count does not match method the "
                    + "parameter count of method '" + method.getName() + "'. Argument count = "
                    + args.length + ", method parameter count = " + paramTypes.length, messageContext);
        }
        Object[] processedArgs = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; ++i) {
            if (args[i] == null || paramTypes[i].isAssignableFrom(args[i].getClass())) {
                //TODO
                processedArgs[i] = args[i];
            } else if (SimpleTypeMapper.isSimpleType(paramTypes[i])) {
                try {
                    processedArgs[i] = SimpleTypeMapper.getSimpleTypeObject(paramTypes[i],
                            AXIOMUtil.stringToOM("<a>" + args[i].toString() + "</a>"));
                } catch (XMLStreamException e) {
                    handleException("XMLStreamException ", e, messageContext);
                }
            } else {
                handleException("Incompatible argument found in " + i + "th argument "
                        + "for '" + method.getName() + "' method.", messageContext);
            }
        }
        try {
            return method.invoke(instance, processedArgs);
        } catch (IllegalAccessException e) {
            handleException("Error while invoking '" + method.getName() + "' method "
                    + "via reflection.", e, messageContext);
        } catch (InvocationTargetException e) {
            handleException("Error while invoking '" + method.getName() + "' method "
                    + "via reflection.", e, messageContext);
        }
        return null;
    }


    /**
     * @param aClass         class of our target method
     * @param methodName     name of the method
     * @param messageContext messageContext
     * @param argCount       number of arguments
     * @return extract the value's from properties and make its as hashable
     */
    public Method resolveMethod(Class aClass, String methodName, int argCount, MessageContext messageContext) {
        Method resolvedMethod = null;
        for (Method method : aClass.getMethods()) {
            if (method.getName().equals(methodName) &&
                    method.getParameterTypes().length == argCount) {

                if (resolvedMethod == null) {
                    resolvedMethod = method;
                } else {
                    handleException("More than one '" + methodName + "' methods " +
                            "that take " + argCount + " arguments are found in '" +
                            aClass.getName() + "' class.", messageContext);
                }
            }
        }
        return resolvedMethod;
    }


    /**
     * @param messageContext message contest
     * @param operationName     name of the operation
     * @return extract the value's from properties and make its as hashable
     */
    public Object[] buildArguments(MessageContext messageContext, String operationName) {
        Hashtable<String, String> dyValues = getParameters(messageContext, operationName);
        Set<String> set = dyValues.keySet();
        Object[] args = new Object[dyValues.size()];
        int i = 0;
        for (String aSet : set) {
            args[i] = dyValues.get(aSet);
            i++;
        }
        return args;
    }


    /**
     * @param messageContext message contest
     * @param operationName     Name of the operation
     * @return extract the value's from properties and make its as hashable
     */
    public Hashtable getParameters(MessageContext messageContext, String operationName) {
        Hashtable dyValues = new Hashtable();
        try {
            String key = "";
            key = operationName + ":" + key.concat((String) getParameter(messageContext, EJBConstance.KEY));
            Map<String, Object> chk;
            chk = (((Axis2MessageContext) messageContext).getProperties());
            Set prop = messageContext.getPropertyKeySet();
            Value val;
            String[] arrayString = (String[]) prop.toArray(new String[prop.size()]);
            for (String s : arrayString) {
                if (s.startsWith(key)) {
                    val = (Value) chk.get(s);
                    dyValues.put(s.substring(key.length() + 1, s.length()), val.getKeyValue());
                }
            }
        } catch (Exception e) {
            handleException(e.getMessage(), e, messageContext);
        }
        return dyValues;
    }


    protected Object getParameter(MessageContext messageContext, String paramName) {
        return ConnectorUtils.lookupTemplateParamater(messageContext, paramName);
    }

    public void handleException(String msg, MessageContext ctx) {
        ejbException.handleException(msg, ctx);
    }

    public void handleException(String msg, Exception e, MessageContext ctx) {
        ejbException.handleException(msg, e, ctx);
    }


    /**
     * Creates the initial context to be used.
     * @return context to be used for service lookup
     * @throws NamingException
     */
//    public InitialContext getInitialContext(MessageContext messageContext) throws NamingException {
//        log.info("Initializing EJBConnector InitialContext ");
//        Properties prop = new Properties();
//        prop=(Properties)messageContext.getProperty("raj");
//        return new InitialContext(prop);
//    }


    /**
     * @param messageContext messageContext
     * @param jndiName       jndi name
     * @return ejb remote object
     */
    public Object getEJBObject(MessageContext messageContext, String jndiName) {
        EJB2Init ejb2Init = new EJB2Init();
        InitialContext context;
        Object obj = null;
        try {
          //  context = ejb2Init.getInitialContext();
            context=new InitialContext(getParameters(messageContext,EJBConstance.CONTEXT));
            obj = context.lookup(getParameter(messageContext, jndiName).toString());
        } catch (NoClassDefFoundError e) {
            handleException("Failed lookup because of ", messageContext);
        } catch (NamingException e) {
            handleException("Failed lookup because of ", e, messageContext);
        }
        EJBHome beanHome =
                (EJBHome) javax.rmi.PortableRemoteObject.narrow(obj, EJBHome.class);
        Method m = null;
        try {
            m = beanHome.getClass().getDeclaredMethod(EJBConstance.CREATE);
        } catch (NoSuchMethodException e) {
            handleException("There is no create method ", messageContext);
        }
        Object ejbObj = null;
        try {
            if (m != null) {
                ejbObj = m.invoke(beanHome);
            } else {
                handleException("ejb home is missing", messageContext);
            }
        } catch (IllegalAccessException e) {
            handleException("", e, messageContext);
        } catch (InvocationTargetException e) {
            handleException("", e, messageContext);
        }
        return ejbObj;
    }
}