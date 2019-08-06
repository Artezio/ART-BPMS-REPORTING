package com.artezio.reporting.yarg;

import com.haulmont.yarg.structure.xml.impl.DefaultXmlReader;
import org.camunda.bpm.engine.ProcessEngine;

import java.io.InputStream;

public class CamundaDeploymentXmlReader extends DefaultXmlReader {

    private final ProcessEngine processEngine;
    private final String deploymentId;

    public CamundaDeploymentXmlReader(ProcessEngine processEngine, String deploymentId) {
        this.processEngine = processEngine;
        this.deploymentId = deploymentId;
    }

    @Override
    protected InputStream getDocumentContent(String documentPath) {
        return processEngine.getRepositoryService().getResourceAsStream(deploymentId, documentPath);
    }
}
