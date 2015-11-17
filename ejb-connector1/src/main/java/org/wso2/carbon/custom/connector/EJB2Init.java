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
import org.wso2.carbon.connector.core.ConnectException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class EJB2Init extends AbstractConnector {
    private final Log log = LogFactory.getLog(EJB2Init.class);
    private final Properties prop = new Properties();
    EJBFactory ejbFactory = new EJBFactory();

    /**
     * Creates the initial context to be used.
     *
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
            Hashtable dyValues = ejbFactory.getParameters(messageContext, EJBConstance.INIT);
            Set set = dyValues.entrySet();
            for (Object aSet : set) {
                Map.Entry entry = (Map.Entry) aSet;
                prop.setProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        } catch (Exception e) {
            handleException("error while set the properties", e, messageContext);
        }

    }



}
