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

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.databinding.typemapping.SimpleTypeMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.Value;
import org.wso2.carbon.connector.core.util.ConnectorUtils;

import javax.ejb.EJBHome;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EJBFactory {
    protected static final Log trace = LogFactory.getLog("TRACE_LOGGER");
    protected int traceState = 2;
    /**
     * Argument list for the remote method invocation.
     */
    private final List<Value> argumentList = new ArrayList<Value>();
    private final Log log = LogFactory.getLog(EJBFactory.class);

    public Object invokeInstanceMethod(Object instance, Method method, Object[] args) throws XMLStreamException {
        Class[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != args.length) {
            log.error("Provided argument count does not match method the "
                    + "parameter count of method '" + method.getName() + "'. Argument count = "
                    + args.length + ", method parameter count = " + paramTypes.length);
        }
        Object[] processedArgs = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; ++i) {
            if (args[i] == null || paramTypes[i].isAssignableFrom(args[i].getClass())) {
                processedArgs[i] = args[i];
            } else if (SimpleTypeMapper.isSimpleType(paramTypes[i])) {
                processedArgs[i] = SimpleTypeMapper.getSimpleTypeObject(paramTypes[i],
                        AXIOMUtil.stringToOM("<a>" + args[i].toString() + "</a>"));
            } else {
                log.error("Incompatible argument found in " + i + "th argument "
                        + "for '" + method.getName() + "' method.");
            }
        }
        try {
            return method.invoke(instance, processedArgs);
        } catch (IllegalAccessException e) {
            log.error("Error while invoking '" + method.getName() + "' method "
                    + "via reflection.");
        } catch (InvocationTargetException e) {
            log.error("Error while invoking '" + method.getName() + "' method "
                    + "via reflection.");
        }
        return null;
    }

    public Method resolveMethod(Class clazz, String methodName, int argCount) {
        Method resolvedMethod = null;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) &&
                    method.getParameterTypes().length == argCount) {

                if (resolvedMethod == null) {
                    resolvedMethod = method;
                } else {
                    log.error("More than one '" + methodName + "' methods " +
                            "that take " + argCount + " arguments are found in '" +
                            clazz.getName() + "' class.");
                }
            }
        }
        return resolvedMethod;
    }


    public Object[] buildArguments(MessageContext synCtx, String methodName) {
        log.info("inside build arg");
        Hashtable dyValues = getParameters(synCtx, methodName);
        Object[] args = new Object[dyValues.size()];
        Set set = dyValues.entrySet();
        int i = 0;
        for (Object aSet : set) {
            Map.Entry entry = (Map.Entry) aSet;
            args[i] = entry.getValue();
            i++;
        }
        return args;

    }

    /**
     * @param ctx        message contest
     * @param methodName Name of the operation
     * @return extract the value's from properties and make its as hashable
     */
    public Hashtable getParameters(MessageContext ctx, String methodName) {
        Hashtable dyValues = new Hashtable();
        try {
            String key = "";
            key = methodName + ":" + key.concat((String) getParameter(ctx, EJBConstance.KEY));
            Map<String, Object> chk;
            chk = (((Axis2MessageContext) ctx).getProperties());
            Set prop = ctx.getPropertyKeySet();
            Value val;
            String[] arrayString = (String[]) prop.toArray(new String[prop.size()]);
            for (String s : arrayString) {
                if (s.startsWith(key)) {
                    val = (Value) chk.get(s);
                    dyValues.put(s.substring(key.length() + 1, s.length()), val.getKeyValue());
                    //log.info(s.substring(j + 1, i) + " : " + val.getKeyValue());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return dyValues;
    }

    protected Object getParameter(MessageContext messageContext, String paramName) {
        return ConnectorUtils.lookupTemplateParamater(messageContext, paramName);
    }

    protected void handleException(String msg, Exception e, MessageContext msgContext) {
        this.log.error(msg, e);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg, e);
        }
        if (this.shouldTrace(msgContext.getTracingState())) {
            trace.error(msg, e);
        }
        throw new SynapseException(msg, e);
    }

    protected void handleException(String msg, MessageContext msgContext) {
        this.log.error(msg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg);
        }
        if (this.shouldTrace(msgContext.getTracingState())) {
            trace.error(msg);
        }
        throw new SynapseException(msg);
    }

    public boolean shouldTrace(int parentTraceState) {
        return this.traceState == 1 || this.traceState == 2 && parentTraceState == 1;
    }

    public Object getEJBObject(MessageContext ctx,String jndiName) {
        EJB2Init ejb2Init = new EJB2Init();
        InitialContext context;
        Object obj = null;
        try {
            context = ejb2Init.getInitialContext();
            obj = context.lookup(getParameter(ctx, jndiName).toString());
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
            if (m != null) {
                ejbObj = m.invoke(beanHome);
            } else {
                handleException("ejb home is missing", ctx);
            }
        } catch (IllegalAccessException e) {
            handleException("", e, ctx);
        } catch (InvocationTargetException e) {
            handleException("", e, ctx);
        }
        return ejbObj;
    }

}