package com.artezio.bpm.reporting.yarg;

import com.artezio.bpm.reporting.yarg.model.Item;
import com.sun.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleResolver;
import junitx.util.PrivateAccessor;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.easymock.TestSubject;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.*;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class YargDelegateTest {

    @Rule
    public ProcessEngineRule processEngineRule = new ProcessEngineRule();
    @Rule
    public WeldInitiator weld = WeldInitiator.fromTestPackage()
            .activate()
            .build();

    @Produces
    public ELResolver getElResolver() {
        return new SimpleResolver(true);
    }

    @Produces
    public ExpressionFactory getExpressionFactory() {
        return new ExpressionFactoryImpl();
    }

    @TestSubject
    private YargDelegate yargDelegate = new YargDelegate();
    private Map<String, Object> variables = new HashMap<>();

    {
        Mocks.register("yargDelegate", yargDelegate);
    }

    @Before
    public void init() throws NoSuchFieldException {
        variables.put("startedBy", "TestUser");
        PrivateAccessor.setField(yargDelegate, "processEngine", processEngineRule.getProcessEngine());
        PrivateAccessor.setField(yargDelegate, "repositoryService", processEngineRule.getRepositoryService());
    }

    @After
    public void tearDown() {
        variables.clear();
        Set<String> taskIds = processEngineRule.getTaskService().createTaskQuery()
                .list()
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        processEngineRule.getHistoryService().createHistoricTaskInstanceQuery()
                .finished()
                .list()
                .forEach(historicTaskInstance -> taskIds.add(historicTaskInstance.getId()));

        taskIds.forEach(taskId -> processEngineRule.getHistoryService().deleteHistoricTaskInstance(taskId));
        taskIds.stream()
                .map(taskId -> processEngineRule.getTaskService().createTaskQuery().taskId(taskId).singleResult())
                .filter(Objects::nonNull)
                .map(Task::getProcessDefinitionId)
                .filter(Objects::nonNull)
                .forEach(processDefinitionId -> processEngineRule.getRepositoryService().deleteProcessDefinition(processDefinitionId, true));
        processEngineRule.getTaskService().deleteTasks(taskIds);

        List<ProcessInstance> processInstances = processEngineRule.getRuntimeService().createProcessInstanceQuery().list();
        processInstances.forEach(processInstance ->
                processEngineRule.getRuntimeService().suspendProcessInstanceById(processInstance.getId()));
    }

    @Test
    @Deployment(resources = {"generateTestReport.bpmn",
            "templates/testTemplate1.docx", "templates/testTemplate1.docx.xml",
            "templates/testTemplate2.docx", "templates/testTemplate2.docx.xml",
            "templates/testTemplate3.xls", "templates/testTemplate3.xls.xml",
            "templates/testTemplate4.xlsx", "templates/testTemplate4.xlsx.xml",
    })
    public void testGenerateTestReports() throws IOException {
        Item book = new Item("Book", 2.0);
        Item umbrella = new Item("Umbrella", 35.0);
        Item extraItem = new Item("Sugar", 3.0);

        variables.put("itemsForSale", Arrays.asList(book, umbrella));
        variables.put("extraItem", extraItem);

        ProcessInstanceWithVariables process = processEngineRule.getRuntimeService().createProcessInstanceByKey("GenerateTestReport")
                .setVariables(variables)
                .executeWithVariablesInReturn();

        FileValue generatedDocReport = process.getVariables().getValueTyped("docxReport");
        FileValue generatedXlsReport = process.getVariables().getValueTyped("xlsReport");
        FileValue generatedXlsxReport = process.getVariables().getValueTyped("xlsxReport");
        assertNotNull(generatedDocReport);
        assertNotNull(generatedXlsReport);
        assertNotNull(generatedXlsxReport);

        // To manually inspect generated files, go to %TEMP% dir and open 'generated-yarg-test-reports' directory
        Path tempDirectory = Files.createTempDirectory("generated-yarg-test-reports");
        Arrays.asList(generatedDocReport, generatedXlsReport, generatedXlsxReport).forEach(report -> {
            try {
                Files.write(tempDirectory.resolve(report.getFilename()), IOUtils.toByteArray(report.getValue()), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
