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

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

public class EJB2Init extends AbstractConnector {
    private final Properties prop = new Properties();
    EJBUtil ejbUtil = new EJBUtil();

    /**
     * Creates the initial context to be used.
     * @return context to be used for service lookup
     * @throws NamingException
     */
    public InitialContext getInitialContext() throws NamingException {
        log.info("Initializing EJBConnector InitialContext");
        return new InitialContext(prop);
    }

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        try {
            Hashtable<String, String> dyValues = ejbUtil.getParameters(messageContext, EJBConstance.INIT);
            Set<String> set = dyValues.keySet();
            for (String aSet : set) {
                prop.setProperty(aSet, dyValues.get(aSet));
            }
           messageContext.setProperty(getParameter(messageContext,EJBConstance.KEY).toString(),prop);
            messageContext.setProperty(messageContext.getMessageID(), null);
        } catch (Exception e) {
            handleException("error while set the properties ", e, messageContext);
        }
    }
}
