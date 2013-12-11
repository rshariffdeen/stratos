/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.message.receiver.health;

import com.google.gson.stream.JsonReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterMonitor;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.SpawningException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import javax.jms.TextMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;


/**
 * A thread for processing topology messages and updating the topology data structure.
 */
public class HealthEventMessageDelegator implements Runnable {

    private static final Log log = LogFactory.getLog(HealthEventMessageDelegator.class);

    @Override
    public void run() {
        log.info("Health event message delegator started");

        while (true) {
            try {
                TextMessage message = HealthEventQueue.getInstance().take();

                String messageText = message.getText();
                if (log.isDebugEnabled())
                    log.debug("Health event message received: [message] " + messageText);

                Event event = jsonToEvent(messageText);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Received event: [event-name] %s", event.getEventName()));
                }

                if (Constants.AVERAGE_REQUESTS_IN_FLIGHT.equals(event.getEventName())) {
                    String clusterId = event.getProperties().get("cluster_id");
                    String partitionId = event.getProperties().get("partition_id");
                    String value = event.getProperties().get("value");
                    String networkPartitionId = PartitionManager.getInstance().getNetworkPartitionOfPartition(partitionId).getId();
                    Float floatValue = Float.parseFloat(value);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("%s event: [cluster] %s [partition] %s [value] %s", event.getEventName(), clusterId, partitionId, value));
                    }

                    AutoscalerContext.getInstance().getMonitor(clusterId).getNetworkPartitionCtxt(networkPartitionId)
                            .setAverageRequestsInFlight(floatValue);

                } else if (Constants.GRADIENT_OF_REQUESTS_IN_FLIGHT.equals(event.getEventName())) {
                    String clusterId = event.getProperties().get("cluster_id");
                    String partitionId = event.getProperties().get("partition_id");
                    String value = event.getProperties().get("value");
                    String networkPartitionId = PartitionManager.getInstance().getNetworkPartitionOfPartition(partitionId).getId();
                    Float floatValue = Float.parseFloat(value);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("%s event: [cluster] %s [partition] %s [value] %s", event.getEventName(), clusterId, partitionId, value));
                    }

                    AutoscalerContext.getInstance().getMonitor(clusterId).getNetworkPartitionCtxt(networkPartitionId)
                            .setRequestsInFlightGradient(floatValue);

                } else if (Constants.SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT.equals(event.getEventName())) {
                    String clusterId = event.getProperties().get("cluster_id");
                    String partitionId = event.getProperties().get("partition_id");
                    String value = event.getProperties().get("value");
                    String networkPartitionId = PartitionManager.getInstance().getNetworkPartitionOfPartition(partitionId).getId();
                    Float floatValue = Float.parseFloat(value);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("%s event: [cluster] %s [partition] %s [value] %s", event.getEventName(), clusterId, partitionId, value));
                    }

                    AutoscalerContext.getInstance().getMonitor(clusterId).getNetworkPartitionCtxt(networkPartitionId)
                            .setRequestsInFlightSecondDerivative(floatValue);

                } else if (Constants.MEMBER_FAULT_EVENT_NAME.equals(event.getEventName())) {
                    String clusterId = event.getProperties().get("cluster_id");
                    String memberId = event.getProperties().get("member_id");

                    if (memberId == null || memberId.isEmpty()) {
                        if(log.isErrorEnabled()) {
                            log.error("Member id not found in received message");
                        }
                    } else {
                        handleMemberFaultEvent(clusterId, memberId);
                    }
                } else if(Constants.AVERAGE_LOAD_AVERAGE.equals(event.getEventName())) {
                    LoadAverage loadAverage = findLoadAverage(event);
                    if(loadAverage != null) {
                        String value = event.getProperties().get("value");
                        Float floatValue = Float.parseFloat(value);
                        loadAverage.setAverage(floatValue);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("%s event: [member] %s [value] %s", event.getProperties().get("member_id"), value));
                        }
                    }
                } else if(Constants.SECOND_DERIVATIVE_OF_LOAD_AVERAGE.equals(event.getEventName())) {
                    LoadAverage loadAverage = findLoadAverage(event);
                    if(loadAverage != null) {
                        String value = event.getProperties().get("value");
                        Float floatValue = Float.parseFloat(value);
                        loadAverage.setSecondDerivative(floatValue);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("%s event: [member] %s [value] %s", event.getProperties().get("member_id"), value));
                        }
                    }
                } else if(Constants.GRADIENT_LOAD_AVERAGE.equals(event.getEventName())) {
                    LoadAverage loadAverage = findLoadAverage(event);
                    if(loadAverage != null) {
                        String value = event.getProperties().get("value");
                        Float floatValue = Float.parseFloat(value);
                        loadAverage.setGradient(floatValue);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("%s event: [member] %s [value] %s", event.getProperties().get("member_id"), value));
                        }
                    }
                } else if(Constants.AVERAGE_MEMORY_CONSUMPTION.equals(event.getEventName())) {
                    MemoryConsumption memoryConsumption = findMemoryConsumption(event);
                    if(memoryConsumption != null) {
                        String value = event.getProperties().get("value");
                        Float floatValue = Float.parseFloat(value);
                        memoryConsumption.setAverage(floatValue);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("%s event: [member] %s [value] %s", event.getProperties().get("member_id"), value));
                        }
                    }
                } else if(Constants.SECOND_DERIVATIVE_OF_MEMORY_CONSUMPTION.equals(event.getEventName())) {
                    MemoryConsumption memoryConsumption = findMemoryConsumption(event);
                    if(memoryConsumption != null) {
                        String value = event.getProperties().get("value");
                        Float floatValue = Float.parseFloat(value);
                        memoryConsumption.setSecondDerivative(floatValue);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("%s event: [member] %s [value] %s", event.getProperties().get("member_id"), value));
                        }
                    }
                } else if(Constants.GRADIENT_MEMORY_CONSUMPTION.equals(event.getEventName())) {
                    MemoryConsumption memoryConsumption = findMemoryConsumption(event);
                    if(memoryConsumption != null) {
                        String value = event.getProperties().get("value");
                        Float floatValue = Float.parseFloat(value);
                        memoryConsumption.setGradient(floatValue);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("%s event: [member] %s [value] %s", event.getProperties().get("member_id"), value));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to retrieve the health stat event message.", e);
            }
        }
    }

    private LoadAverage findLoadAverage(Event event) {
        String memberId = event.getProperties().get("member_id");
        Member member = findMember(memberId);
        if(member != null) {
            String networkPartitionId = PartitionManager.getInstance().getNetworkPartitionOfPartition(member.getPartitionId()).getId();
            LoadAverage loadAverage = AutoscalerContext.getInstance().getMonitor(member.getClusterId())
                    .getNetworkPartitionCtxt(networkPartitionId)
                    .getPartitionCtxt(member.getPartitionId())
                    .getMemberStatsContext(memberId).getLoadAverage();

            if(loadAverage == null) {
                loadAverage = new LoadAverage();
                AutoscalerContext.getInstance().getMonitor(member.getClusterId())
                        .getNetworkPartitionCtxt(networkPartitionId)
                        .getPartitionCtxt(member.getPartitionId())
                        .getMemberStatsContext(memberId).setLoadAverage(loadAverage);
            }
            return loadAverage;
        }
        else {
            if(log.isErrorEnabled()) {
                log.error(String.format("Member not found: [member] %s", memberId));
            }
            return null;
        }
    }

    private MemoryConsumption findMemoryConsumption(Event event) {
        String memberId = event.getProperties().get("member_id");
        Member member = findMember(memberId);
        if(member != null) {
            String networkPartitionId = PartitionManager.getInstance().getNetworkPartitionOfPartition(member.getPartitionId()).getId();
            MemoryConsumption memoryConsumption = AutoscalerContext.getInstance().getMonitor(member.getClusterId())
                    .getNetworkPartitionCtxt(networkPartitionId)
                    .getPartitionCtxt(member.getPartitionId())
                    .getMemberStatsContext(memberId).getMemoryConsumption();

            if(memoryConsumption == null) {
                memoryConsumption = new MemoryConsumption();
                AutoscalerContext.getInstance().getMonitor(member.getClusterId())
                        .getNetworkPartitionCtxt(networkPartitionId)
                        .getPartitionCtxt(member.getPartitionId())
                        .getMemberStatsContext(memberId).setMemoryConsumption(memoryConsumption);
            }
            return memoryConsumption;
        }
        else {
            if(log.isErrorEnabled()) {
                log.error(String.format("Member not found: [member] %s", memberId));
            }
            return null;
        }
    }

    private Member findMember(String memberId) {
        try {
            TopologyManager.acquireReadLock();
            for(Service service : TopologyManager.getTopology().getServices()) {
                for(Cluster cluster : service.getClusters()) {
                    if(cluster.memberExists(memberId)) {
                        return cluster.getMember(memberId);
                    }
                }
            }
            return null;
        }
        finally {
            TopologyManager.releaseReadLock();
        }
    }

    private void handleMemberFaultEvent(String clusterId, String memberId) {
        try {

            ClusterMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
            if (!monitor.memberExist(memberId)) {
                // member has already terminated. So no action required
                return;
            }

            // terminate the faulty member
            CloudControllerClient ccClient = CloudControllerClient.getInstance();
            ccClient.terminate(memberId);

            // start a new member in the same Partition
            String partitionId = monitor.getPartitonOfMember(memberId);
            Partition partition = monitor.getDeploymentPolicy().getPartitionById(partitionId);
            ccClient.spawnAnInstance(partition, clusterId);
            if (log.isInfoEnabled()) {
                log.info(String.format("Instance spawned for fault member: [partition] %s [cluster] %s", partitionId, clusterId));
            }

        } catch (TerminationException e) {
            log.error(e);
        } catch (SpawningException e) {
            log.error(e);
        }
    }

    public Event jsonToEvent(String json) {

        Event event = new Event();
        BufferedReader bufferedReader = new BufferedReader(new StringReader(json));
        JsonReader reader = new JsonReader(bufferedReader);
        try {
            reader.beginObject();

            if (reader.hasNext()) {
                event.setEventName(reader.nextName());

                reader.beginObject();
                Map<String, String> properties = new HashMap<String, String>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    String value = reader.nextString();
                    properties.put(name, value);
                }
                event.setProperties(properties);
            }
            reader.close();
            return event;
        } catch (IOException e) {
            log.error("Could not extract event");
        }
        return null;
    }

    private class Event {
        private String eventName;
        private Map<String, String> properties;

        private String getEventName() {
            return eventName;
        }

        private void setEventName(String eventName) {
            this.eventName = eventName;
        }

        private Map<String, String> getProperties() {
            return properties;
        }

        private void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }
}
