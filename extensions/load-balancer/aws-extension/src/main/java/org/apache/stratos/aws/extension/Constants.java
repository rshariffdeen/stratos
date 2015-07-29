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

package org.apache.stratos.aws.extension;

/**
 * AWS proxy extension constants.
 */
public class Constants {
    public static final String CEP_STATS_PUBLISHER_ENABLED = "cep.stats.publisher.enabled";
    public static final String THRIFT_RECEIVER_IP = "thrift.receiver.ip";
    public static final String THRIFT_RECEIVER_PORT = "thrift.receiver.port";
    public static final String NETWORK_PARTITION_ID = "network.partition.id";
    public static final String CLUSTER_ID = "cluster.id";
    public static final String SERVICE_NAME = "service.name";
    public static final String AWS_PROPERTIES_FILE="aws.properties.file";
    public static final String AWS_ACCESS_KEY = "access-key";
    public static final String AWS_SECRET_KEY = "secret-key";
    public static final String LB_PREFIX = "load-balancer-prefix";
    public static final String LOAD_BALANCER_SECURITY_GROUP_NAME = "load-balancer-security-group-name";
    public static final String LOAD_BALANCER_SECURITY_GROUP_DESCRIPTION = "Security group for load balancers created for Apache Stratos.";
}
