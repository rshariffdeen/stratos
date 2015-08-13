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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.CommandUtils;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.domain.Member;
import org.apache.stratos.load.balancer.common.domain.Port;
import org.apache.stratos.load.balancer.common.domain.Service;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AWS statistics reader.
 */
public class AWSStatisticsReader implements LoadBalancerStatisticsReader {

	private static final Log log = LogFactory.getLog(AWSStatisticsReader.class);

	private TopologyProvider topologyProvider;
	private String clusterInstanceId;

	private AWSHelper awsHelper;

	public AWSStatisticsReader(TopologyProvider topologyProvider)
			throws LoadBalancerExtensionException {
		this.topologyProvider = topologyProvider;
		this.clusterInstanceId = System.getProperty(
				StratosConstants.CLUSTER_INSTANCE_ID,
				StratosConstants.NOT_DEFINED);

		awsHelper = new AWSHelper();
	}

	@Override
	public String getClusterInstanceId() {
		return clusterInstanceId;
	}

	@Override
	public int getInFlightRequestCount(String clusterId) {

		int inFlightRequestCount = 0;

		ConcurrentHashMap<String, LoadBalancerInfo> clusterIdToLoadBalancerMap = AWSLoadBalancer
				.getClusterIdToLoadBalancerMap();

		if (clusterIdToLoadBalancerMap.containsKey(clusterId)) {
			LoadBalancerInfo loadBalancerInfo = clusterIdToLoadBalancerMap
					.get(clusterId);

			String loadBalancerName = loadBalancerInfo.getName();
			String region = loadBalancerInfo.getRegion();

			inFlightRequestCount = awsHelper.getRequestCount(loadBalancerName,
					region, awsHelper.getStatisticsInterval())
					- awsHelper.getAllResponsesCount(loadBalancerName, region,
							awsHelper.getStatisticsInterval());

			if (inFlightRequestCount < 0)
				inFlightRequestCount = 0;

		}

		return inFlightRequestCount;
	}
}
