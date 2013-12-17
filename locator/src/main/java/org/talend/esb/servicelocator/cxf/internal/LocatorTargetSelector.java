/*
 * #%L
 * Service Locator Client for CXF
 * %%
 * Copyright (C) 2011 - 2012 Talend Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.talend.esb.servicelocator.cxf.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.clustering.FailoverTargetSelector;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;

public class LocatorTargetSelector extends FailoverTargetSelector {

    private static final Logger LOG = Logger.getLogger(LocatorTargetSelector.class.getPackage().getName());

    private static final String LOCATOR_PROTOCOL = "locator://";

    private static final String LOCATOR_SETADDRESS_FLAG = "setAddress.called";

    private boolean locatorProtocol;

    private LocatorSelectionStrategy strategy = new DefaultSelectionStrategy();

    @Override
    public synchronized Conduit selectConduit(Message message) {
        setAddress(message);
        return super.selectConduit(message);
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.clustering.FailoverTargetSelector#prepare(org.apache.cxf.message.Message)
     */
    @Override
    public synchronized void prepare(Message message) {
        setAddress(message);
        super.prepare(message);
    }

    protected void setAddress(Message message) {
        if(message.getExchange().get(LOCATOR_SETADDRESS_FLAG) != Boolean.TRUE) {
            EndpointInfo ei = endpoint.getEndpointInfo();
            if (locatorProtocol || ei.getAddress().startsWith(LOCATOR_PROTOCOL)) {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.log(Level.INFO, "Found address with locator protocol, mapping it to physical address.");
                    LOG.log(Level.INFO, "Using strategy " + strategy.getClass().getName() + ".");
                }

                String physAddress = strategy.getPrimaryAddress(message.getExchange());

                if (physAddress != null) {
                    ei.setAddress(physAddress);
                    locatorProtocol = true;
                    message.put(Message.ENDPOINT_ADDRESS, physAddress);
                } else {
                    if (LOG.isLoggable(Level.SEVERE)) {
                        LOG.log(Level.SEVERE, "Failed to map logical locator address to physical address.");
                    }
                    throw new IllegalStateException("No endpoint found in Service Locator for service "
                            + endpoint.getService().getName());
                }

            }
            message.getExchange().put(LOCATOR_SETADDRESS_FLAG, Boolean.TRUE);
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Setting address was skipped. It was already applied for this message");
            }
        }
    }

    public void setLocatorSelectionStrategy(LocatorSelectionStrategy locatorSelectionStrategy) {
        strategy = locatorSelectionStrategy;
        setStrategy(locatorSelectionStrategy);
    }

}
