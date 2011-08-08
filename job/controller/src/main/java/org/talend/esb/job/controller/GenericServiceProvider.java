/*
 * #%L
 * Talend :: ESB :: Job :: Controller
 * %%
 * Copyright (C) 2011 Talend Inc.
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
package org.talend.esb.job.controller;

import java.util.Map;

import org.talend.esb.job.controller.ESBEndpointConstants.OperationStyle;
import org.talend.esb.job.controller.internal.ESBProviderBase;
import org.talend.esb.job.controller.internal.RuntimeESBProviderCallback;

import routines.system.api.ESBEndpointInfo;
import routines.system.api.ESBProviderCallback;

public class GenericServiceProvider extends ESBProviderBase {

    private Map<String, String> operations;
    private JobLauncher jobLauncher;

    public void setOperations(Map<String, String> operations) {
        this.operations = operations;
    }

    public void setJobLauncher(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }

    @Override
    public RuntimeESBProviderCallback getESBProviderCallback(String operationName) {
        RuntimeESBProviderCallback esbProviderCallback =
            super.getESBProviderCallback(operationName);
        if (null == esbProviderCallback) {
            final String jobName = operations.get(operationName);
            if (jobName == null) {
                throw new RuntimeException("Job for operation '" + operationName + "' not found");
            }
            esbProviderCallback =
                createESBProviderCallback(operationName, isOperationRequestResponse(operationName));
            jobLauncher.startJob(jobName,
                new GenericESBProviderCallbackController(
                    operationName, isOperationRequestResponse(operationName),
                    esbProviderCallback));
        }
        return esbProviderCallback;
    }

    class GenericESBProviderCallbackController implements ESBProviderCallbackController {

        private final String operationName;
        private final boolean isRequestResponse;
        private final RuntimeESBProviderCallback esbProviderCallback;

        public GenericESBProviderCallbackController(
            String operationName,
            boolean isRequestResponse,
            RuntimeESBProviderCallback esbProviderCallback) {
            this.operationName = operationName;
            this.isRequestResponse = isRequestResponse;
            this.esbProviderCallback = esbProviderCallback;
        }

        public ESBProviderCallback createESBProviderCallback(
            final ESBEndpointInfo esbEndpointInfo) {
            if(!operationName.equals(
                    (String)esbEndpointInfo.getEndpointProperties().get(
                        ESBEndpointConstants.DEFAULT_OPERATION_NAME))) {
                throw new IllegalArgumentException("Different operations found");
            }
            if(isRequestResponse !=
                OperationStyle.isRequestResponse(
                    (String)esbEndpointInfo.getEndpointProperties().get(
                        ESBEndpointConstants.COMMUNICATION_STYLE))) {
                throw new IllegalArgumentException("Found incompatible communication styles");
            }
            return esbProviderCallback;
        }

        public void destroyESBProviderCallback() {
            GenericServiceProvider.this.destroyESBProviderCallback(operationName);
        }

        public boolean isRequired() {
            return true;
        }

    }

}