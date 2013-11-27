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
import org.apache.stratos.cloud.controller.axiom.parser.CartridgeConfigParser;
import org.apache.stratos.cloud.controller.concurrent.ThreadExecutor;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.MalformedConfigurationFileException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.util.*;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * All the {@link org.apache.stratos.cloud.controller.util.Cartridge}s will get deployed / undeployed / updated via this class.
 */
public class CartridgeDeployer extends AbstractDeployer{
    
    private static final Log log = LogFactory.getLog(CartridgeDeployer.class);
    
    private FasterLookUpDataHolder serviceContextLookUpStructure;
    private Map<String, List<Cartridge>> fileToCartridgeListMap;
    private File cartridgesSchema, cartridgeSchema;

    public void init(ConfigurationContext arg0) {
        fileToCartridgeListMap = new ConcurrentHashMap<String, List<Cartridge>>();
        String etcDir = CarbonUtils.getCarbonConfigDirPath() + File.separator + "etc" + File.separator;
        cartridgesSchema = new File(etcDir+"cartridges.xsd");
        cartridgeSchema = new File(etcDir+"cartridge.xsd");
    }

    public void setDirectory(String arg0) {
        // component xml handles this
    }

    public void setExtension(String arg0) {
        // component xml handles this
    }
    
    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {
        
        log.debug("Started to deploy the deployment artifact: "+deploymentFileData.getFile());

        try {
            OMElement docElt = AxiomXpathParserUtil.parse(deploymentFileData.getFile());
            String fileName = deploymentFileData.getFile().getAbsolutePath();
            
        	// validate
            validateCartridge(docElt, fileName);
            
			// deploy - grab cartridges
			List<Cartridge> cartridges = CartridgeConfigParser.parse(fileName, docElt);

			// update map
			fileToCartridgeListMap.put(deploymentFileData.getAbsolutePath(),
			                           new ArrayList<Cartridge>(cartridges));
			
			ThreadExecutor exec = new ThreadExecutor();
			// create Jclouds objects, for each IaaS
			for (Cartridge cartridge : cartridges) {
				// jclouds object building is time consuming, hence I use Java executor framework
				exec.execute(new JcloudsObjectBuilder(cartridge, deploymentFileData));
			}
			// wait till the jobs finish.
			exec.shutdown();

			TopologyBuilder.handleServiceCreated(cartridges);
			log.info("Successfully deployed the Cartridge definition specified at " + deploymentFileData.getAbsolutePath());
			
        } catch (Exception e) {
			String msg = "Invalid deployment artefact at "+deploymentFileData.getAbsolutePath();
            // back up the file - this will in-turn triggers undeploy()
            File f = deploymentFileData.getFile();
            f.renameTo(new File(deploymentFileData.getAbsolutePath()+".back"));
            log.error(msg, e);
            throw new DeploymentException(msg, e);
		}
    }
    
    private void validateCartridge(final OMElement elt, final String fileName) throws MalformedConfigurationFileException {
        boolean validated = false;
        Exception firstException = null;

        try{
            // first try to validate using cartridges schema
            AxiomXpathParserUtil.validate(elt, cartridgesSchema);
            validated = true;
            
        }catch (Exception e) {
            firstException = e;
        }
        
        if(!validated){
            try{
                // Now try to validate using cartridge schema
                AxiomXpathParserUtil.validate(elt, cartridgeSchema);
                validated = true;
                log.debug("Cartridge validation was successful.");
                
            }catch (Exception e) {
                String msg = "Cartridge XML validation failed. Invalid Cartridge XML: "+fileName;
                log.error(msg, firstException);
                throw new MalformedConfigurationFileException(msg, firstException);
            }
        }
        
        
    }

    public void undeploy(String file) throws DeploymentException {
        
    	serviceContextLookUpStructure = FasterLookUpDataHolder.getInstance();
    	
        // grab the entry from Map
        if(fileToCartridgeListMap.containsKey(file)){
            // remove 'em
            TopologyBuilder.handleServiceRemoved(fileToCartridgeListMap.get(file));
            serviceContextLookUpStructure.removeCartridges(fileToCartridgeListMap.get(file));
            
            log.info("Successfully undeployed the Cartridge definition specified at "+file);
        }
        
    }
    
    private void handleException(String msg, Exception e) {
        log.fatal(msg, e);
        throw new CloudControllerException(msg, e);
    }
    
    /**
     * JcloudsObjectBuilder job
     *
     */
    class JcloudsObjectBuilder implements Runnable{
    	
    	private Cartridge cartridge;
    	private DeploymentFileData file;

    	public JcloudsObjectBuilder (Cartridge cartridge, DeploymentFileData file){
    		this.cartridge = cartridge;
    		this.file = file;
    	}
    	
		@Override
        public void run() {

			for (IaasProvider iaasProvider : cartridge.getIaases()) {
				try {
					Iaas iaas = (Iaas) Class.forName(iaasProvider.getClassName()).newInstance();
					iaas.buildComputeServiceAndTemplate(iaasProvider);
					iaasProvider.setIaas(iaas);
                    if(iaasProvider.getListOfRegions() != null) {
                        for(Region region : iaasProvider.getListOfRegions()) {
                            iaas.buildComputeServiceAndTemplate(region);
                            for(Zone zone : region.getListOfZones()) {
                                zone.setComputeService(region.getComputeService());
                                iaas.buildTemplate(zone);
                                for(Host host: zone.getListOfHosts()) {
                                    host.setComputeService(region.getComputeService());
                                    iaas.buildTemplate(host);
                                }
                            }

                        }
                    }
					
				} catch (Exception e) {
					rename();
					handleException(e.getMessage(), e);
				}
			}
			
        }
		
		private void rename(){
			// back up the file
            File f = file.getFile();
            f.renameTo(new File(file.getAbsolutePath()+".back"));
		}
    	
    }

}
