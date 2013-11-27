/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.deployers;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.AbstractDeployer;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.axiom.AxiomXpathParser;
import org.apache.stratos.cloud.controller.axiom.AxiomXpathParserUtil;
import org.apache.stratos.cloud.controller.axiom.parser.ServiceConfigParser;
import org.apache.stratos.cloud.controller.exception.MalformedConfigurationFileException;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.ServiceContext;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * All the {@link org.apache.stratos.cloud.controller.util.Cartridge}s will get deployed / undeployed / updated via this class.
 */
//public class ServiceDeployer extends AbstractDeployer{
    
//    private static final Log log = LogFactory.getLog(ServiceDeployer.class);
//    
//    private FasterLookUpDataHolder serviceContextLookUpStructure;
//    private Map<String, List<ServiceContext>> fileToServiceContextListMap;
//    private File servicesSchema, serviceSchema;
//
//    @Override
//    public void init(ConfigurationContext arg0) {
//        fileToServiceContextListMap = new ConcurrentHashMap<String, List<ServiceContext>>();
//        String etcDir = CarbonUtils.getCarbonConfigDirPath() + File.separator + "etc" + File.separator;
//        servicesSchema = new File(etcDir+"services.xsd");
//        serviceSchema = new File(etcDir+"service.xsd");
//    }
//
//    @Override
//    public void setDirectory(String arg0) {
//        // component xml handles this
//    }
//
//    @Override
//    public void setExtension(String arg0) {
//        // component xml handles this
//    }
//    
//    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {
//
//        File file = deploymentFileData.getFile();
//        log.debug("Started to deploy the deployment artefact: " + file);
//        serviceContextLookUpStructure = FasterLookUpDataHolder.getInstance();
//
//        try {
//            OMElement docElt = AxiomXpathParserUtil.parse(file);
//            String fileName = file.getAbsolutePath();
//            
//            // validate
//            validateService(docElt, fileName);
//
//            // deploy
//            List<ServiceContext> services = ServiceConfigParser.extractServiceContexts(file, docElt);
//
//            // update map
//            fileToServiceContextListMap.put(deploymentFileData.getAbsolutePath(),
//                                            new ArrayList<ServiceContext>(services));
//
//            log.info("Successfully deployed the Service definition specified at " +
//                     deploymentFileData.getAbsolutePath());
//        } catch (Exception e) {
//            String msg = "Invalid deployment artefact at " + deploymentFileData.getAbsolutePath();
//            // back up the file
//            File f = file;
//            f.renameTo(new File(deploymentFileData.getAbsolutePath() + ".back"));
//            log.error(msg, e);
//            throw new DeploymentException(msg, e);
//        }
//    }
//    
//    private void validateService(final OMElement elt, final String fileName) throws Exception {
//        boolean validated = false;
//        Exception firstException = null;
//
//        try{
//            // first try to validate using services schema
//            AxiomXpathParserUtil.validate(elt, servicesSchema);
//            validated = true;
//            log.debug("Service validation was successful.");
//            
//        }catch (Exception e) {
//            firstException = e;
//        }
//        
//        if(!validated){
//            try{
//                // Now try to validate using service schema
//                AxiomXpathParserUtil.validate(elt, serviceSchema);
//                validated = true;
//                log.debug("Service validation was successful.");
//                
//            }catch (Exception e) {
//                String msg = "Service XML validation failed. Invalid Service XML: "+fileName;
//                log.error(msg, firstException);
//                throw new MalformedConfigurationFileException(msg, firstException);
//            }
//        }
//        
//        
//    }
//
//    public void undeploy(String file) throws DeploymentException {
//
//        serviceContextLookUpStructure = FasterLookUpDataHolder.getInstance();
//
//        // grab the entry from Map
//        if(fileToServiceContextListMap.containsKey(file)){
//            // remove 'em all
//            for (ServiceContext ctxt : fileToServiceContextListMap.get(file)) {
//                serviceContextLookUpStructure.removeServiceContext(ctxt);
//                TopologyBuilder.handleClusterRemoved(ctxt);
//                // remove from the topology
//            }
//            log.info("Successfully undeployed the Service definition specified at "+file);
//        }
//        
//    }

//}
