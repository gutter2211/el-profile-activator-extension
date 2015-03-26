/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.jboss.maven.elprofile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.mvel2.CompileException;
import org.mvel2.MVEL;

/**
 * Alternative implementation of PropertyActivator for Maven 3
 * 
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="mailto:nikolaus.winter@comdirect.de">Nikolaus Winter</a>
 * 
 */
@Component(role = ProfileActivator.class, hint = "property")
public class ElProfileActivator implements ProfileActivator {

    private static final String PROPERTY_FILE_NAME = "profileactivation.properties";
    private static final String MVEL_SCRIPT_PROPERTY_NAME = "mvel";

    @Requirement
    private Logger logger;

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problemCollector) {
        Activation activation = profile.getActivation();

        boolean result = false;

        if (activation != null) {
            ActivationProperty property = activation.getProperty();

            if (property != null) {
                String name = property.getName();

                if (name != null && MVEL_SCRIPT_PROPERTY_NAME.equals(name)) {
                    String value = property.getValue();
                    logger.debug("Evaluating following MVEL expression: " + value);
                    result = evaluateMvel(value, context);
                    logger.debug("Evaluated MVEL expression: " + value + " as " + result);
                }
            }
        }

        // call original implementation if mvel script was not valid/false
        return result ? true : new PropertyProfileActivator().isActive(profile, context, problemCollector);
    }

    private boolean evaluateMvel(String expression, ProfileActivationContext context) {
    	
        if (expression == null || expression.length() == 0) {
            return false;
        }
        
        if(context==null || context.getProjectDirectory()==null || !context.getProjectDirectory().exists()) {
        	return false;
        }

        try {
            // "casting" to <String,Object> and including both user and system properties
            Map<String,Object> externalVariables = new HashMap<String,Object>();
            externalVariables.putAll(loadProfileActivationPropertyFile(context.getProjectDirectory()));
            externalVariables.putAll(context.getSystemProperties());
            externalVariables.putAll(context.getUserProperties());

            return MVEL.evalToBoolean(expression, externalVariables);
        }
        catch (NullPointerException e) {
            logger.warn("Unable to evaluate mvel property value (\"" + expression + "\")");
            logger.debug(e.getMessage());
            return false;
        }
        catch (CompileException e) {
            logger.warn("Unable to evaluate mvel property value (\"" + expression + "\")");
            logger.debug(e.getMessage());
            return false;
        }
    }

    private Map<String,String> loadProfileActivationPropertyFile(File projectDirectory) {
        Map<String,String> propertiesMap = new HashMap<String,String>();

        File propertyFile = new File(projectDirectory, PROPERTY_FILE_NAME);

        if (!propertyFile.exists()) {
            return propertiesMap;
        }

        Properties prop = new Properties();

        try {
            FileInputStream fileStream = new FileInputStream(propertyFile);
            prop.load(fileStream);
            fileStream.close();
            for (Map.Entry<Object,Object> entry : prop.entrySet()) {
                propertiesMap.put(entry.getKey().toString(), entry.getValue().toString());
            }
            logger.debug("property file " + PROPERTY_FILE_NAME + " found with " + propertiesMap.size() + " entries.");
        }
        catch (IOException e) {
            logger.warn("Error while loading file " + PROPERTY_FILE_NAME);
            logger.debug(e.getMessage());
        }

        return propertiesMap;
    }
}
