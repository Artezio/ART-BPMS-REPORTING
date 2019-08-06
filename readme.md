### [Yarg]  reporting for Camunda

#### Features

* [JUEL] expressions support  
* Conditional text rendering based on JUEL expressions

#### Usage

* Build the module and add compiled jar to your Camunda engine classpath (i.e. into EAR or WAR lib folder)
* Install [OpenOffice]
* Assuming that OpenOffice is installed in `$OPENOFFICE` directory, set environment variable `OPENOFFICE_INSTALLATION_DIRECTORY` to point to `OPENOFFICE_INSTALLATION_DIRECTORY/program` subdirectory, for example `OPENOFFICE_INSTALLATION_DIRECTORY=c:/OpenOffice/program`
* Follow the instruction below to create and run reports

#### Creating reports in BPMN

There's an example of BPMN diagram and template files in /src/test/resources folder

* Create Yarg template (docx, xls, etc.) and put it into your Process Application Archive along with BPMN files. See `test/resources/testTemplate1.docx.xml` and `test/resources/testTemplate1.docx`
* For example, let's assume the template is located at `/templates/testTemplate1.docx`
* Create Yarg report definition XML named the same as the template plus `.xml`. Put it into the same directory as template (`/templates/testTemplate1.docx.xml`). The report definition contains:
  * `outputType` - docx, xsl, pdf etc. `docx` in the example
  * `documentPath` - full path to the document inside Process Application Archive. Used as resource reference in BPMN. `templates/testTemplate1.docx` in the example
  * `code` - contains the same value as documentPath (`/templates/template.docx`)
  * `documentName` - a name of the document file `testTemplate1.docx`
  * `outputNamePattern` - output file name, e.g. `testTemplate1-result.docx`
* Add a Service task `Generate report` and point it to `com.artezio.reporting.yarg.YargDelegate` class (e.g. Delegate expression `${yargDelegate}` when using CDI). The task must be configured as follows:
  * Add input parameter named `templates` of type `List`. List templates with full paths, e.g. `templates/testTemplate1.docx` 
  * Add input parameter named `params` of type `Map`. Add all params that are required within Yarg template. The key specifies param name, while the value contains the parameter itself and may contain for example process variable reference: [`param1`: `${someCalculatedValue}`]. Each param will be available in the report for accessing by its key, e.g. param with key `param1` could be accessed from JSON report band query as `parameter=param1 $.someValue`
  * The output of the task is stored in local variable named `generatedReports` which has type Map and contains generated reports. Each map entry has:
    * Key of type String which is report's output filename
    * Value of type FileValue which is generated document file. 
  * To put the result into process variables, create output of type Script with language `juel`, set name to desired Process Variable name and enter one-line script `${generatedReports}`.
    
---

#### Using JUEL expressions

There is a feature to evaluate [JUEL] expressions inside templates

To evaluate an expression, put it with `#{}` brackets anywhere inside DOCX, DOC, XLSX or XLS file:

`#{priceStorage.getItemPrice(${item.id}) + 3}`

You can use band data in an expression as always: `#{${item.price} - ${defaults.defaultPrice}}`

If you need to use quotes (`"`) inside an expression, you would have to switch off smart quotes substitution in your editor. To do that in MS Word, open `File->Options->Proofing->AutoCorret Options` and turn off `Replace Straight quotes with Smart quotes` on each tab

---

#### Using conditional formatting in Yarg templates

There is a feature to display or hide data depending on dynamically calculated condition. This feature is supported for *doc, docx, xls, xlsx* input templates

Condition format:

```
${IF <Juel expression with Boolean result>} 
  <Displayed if expression is True> 
${ELSE} 
  <Displayed if expression is False> 
${ENDIF}
```
`${ELSE}` block is optional

`${IF} ${ENDIF}` blocks are mandatory

Juel expression is any java EL 3.0 expression. Static method invocations are not supported  

When writing conditionals inside **DOCX** templates, the entire IF-ELSE-ENDIF block must reside inside *single paragraph*. DOC, XLS, XLSX templates have no special requirements

If you need to use quotes (`"`) inside an expression, you would have to switch off quotes substitution in your editor. To do that in MS Word, open `File->Options->Proofing->AutoCorret Options` and turn off `Replace Straight quotes with Smart quotes` on each tab   

You can use any references to Yarg band data inside Juel expressions and between IF-ELSE-ENDIF tags, as in the following example 

---
#### DOCX conditional template example
Output text depending on item.value using bean method getMinValue. Then output "true is equal to true"
   
```

   --- paragraph begin ---

   ${IF ${item.value} > bean.getMinValue() }
   The item ${item.name} has value greater than min value 
   ${ELSE}
   The item ${item.name} has value less than min value
   ${ENDIF}
   
   --- paragraph end ---



   --- paragraph begin ---
   
   ${IF true == true} true is equal to true ${ENDIF}
   
   --- paragraph end ---
  
  
```

#### Running included test BPMN scenario with output files

You need [OpenOffice] to run report generation test with output files

Set environment variable `OPENOFFICE_INSTALLATION_DIRECTORY` as stated in Usage section 

Run YargDelegateTest.testGenerateTestReport. All generated files will be written into %TEMP% folder under subdirectory `generated-yarg-test-reports` 

[Yarg]: https://github.com/cuba-platform/yarg
[JUEL]: https://docs.oracle.com/javaee/5/tutorial/doc/bnahq.html
[OpenOffice]: https://www.openoffice.org/ru/
