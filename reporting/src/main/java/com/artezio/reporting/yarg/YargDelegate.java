package com.artezio.reporting.yarg;

import com.artezio.reporting.yarg.formatters.FormatterFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.yarg.formatters.factory.DefaultFormatterFactory;
import com.haulmont.yarg.formatters.factory.ReportFormatterFactory;
import com.haulmont.yarg.loaders.factory.DefaultLoaderFactory;
import com.haulmont.yarg.loaders.factory.ReportLoaderFactory;
import com.haulmont.yarg.loaders.impl.JsonDataLoader;
import com.haulmont.yarg.reporting.ReportOutputDocument;
import com.haulmont.yarg.reporting.Reporting;
import com.haulmont.yarg.reporting.RunParams;
import com.haulmont.yarg.structure.Report;
import com.haulmont.yarg.structure.xml.XmlReader;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
public class YargDelegate implements JavaDelegate {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ReportFormatterFactory formatterFactory = new FormatterFactory();
    private final ReportLoaderFactory loaderFactory = new DefaultLoaderFactory().setJsonDataLoader(new JsonDataLoader());

    @Inject
    private ProcessEngine processEngine;
    @Inject
    private RepositoryService repositoryService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        List<String> templates = (List<String>)execution.getVariable("templates");
        String deploymentId = repositoryService.getProcessDefinition(execution.getProcessDefinitionId()).getDeploymentId();
        Map<String, Object> params = (Map<String, Object>)execution.getVariable("params");
        List<FileValue> generatedReports = templates.stream()
                .map(templateCode -> generateReport(deploymentId, templateCode, params))
                .map(this::toFileValue)
                .collect(Collectors.toList());
        execution.setVariableLocal("generatedReports", generatedReports);
    }

    private FileValue toFileValue(ReportOutputDocument reportOutputDocument) {
        return Variables
                .fileValue(reportOutputDocument.getDocumentName())
                .file(reportOutputDocument.getContent())
                .encoding(Charset.forName("UTF-8"))
                .create();
    }

    private ReportOutputDocument generateReport(String deploymentId, String template, Map<String, Object> params) {
        InputStream definitionXml = repositoryService.getResourceAsStream(deploymentId, template + ".xml");
        XmlReader xmlReader = new CamundaDeploymentXmlReader(processEngine, deploymentId);
        try {
            return generateReportFromDefinition(definitionXml, xmlReader, template, params);
        } catch (IOException e) {
            throw new RuntimeException("Could not generate report", e);
        }
    }

    private ReportOutputDocument generateReportFromDefinition(InputStream definitionXml, XmlReader xmlReader, String template, Map<String, Object> params) throws IOException {
        Report report = xmlReader.parseXml(IOUtils.toString(definitionXml, Charset.forName("UTF-8")));
        RunParams runParams = new RunParams(report).templateCode(template);
        params.forEach((key, value) -> runParams.param(key, serialize(value)));
        return getReporting().runReport(runParams);
    }

    private String serialize(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize object", e);
        }
    }

    private Reporting getReporting() {
        Reporting reporting = new Reporting();
        reporting.setFormatterFactory(formatterFactory);
        reporting.setLoaderFactory(loaderFactory);
        return reporting;
    }
}
