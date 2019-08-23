### [Yarg]  reporting for Camunda

#### Features

* [Yarg] report generation from BPMN
* [JUEL] expressions support  
* Conditional text rendering based on JUEL expressions

#### Installation

* Adding to a **Maven** project:
  * Clone this repository and navigate into its directory
  * Run `mvn clean install`
  * Add the following dependency to your application pom.xml:
      ```
      <dependency>
          <groupId>com.artezio.bpm.camunda.report</groupId>
          <artifactId>yarg</artifactId>
          <version>1.0</version>
      </dependency>
      ```
* Usage in non-maven projects: 
  * Build the module with maven and add compiled jar to your Camunda engine classpath (i.e. into EAR or WAR lib folder)
* Follow the instruction below to create and run reports


**Additional steps are required to generate `.DOC` reports:**

  * Install [OpenOffice]
  * Assuming that OpenOffice is installed in `$OPENOFFICE` directory, set environment variable `OPENOFFICE_INSTALLATION_DIRECTORY` to point to `OPENOFFICE_INSTALLATION_DIRECTORY/program` subdirectory, for example `OPENOFFICE_INSTALLATION_DIRECTORY=c:/OpenOffice/program`


#### Creating reports in BPMN

There's an example of BPMN diagram and template files in /src/test/resources folder

* Create Yarg template (docx, xls, etc.) and put it into your Process Application Archive along with BPMN files. See `test/resources/testTemplate1.docx.xml` and `test/resources/testTemplate1.docx`
* For example, let's assume the template is located at `/templates/testTemplate1.docx`
* Create Yarg report definition XML named the same as the template ending with `.xml`. Put it into the same directory as template (`/templates/testTemplate1.docx.xml`). The report definition contains:
  * `outputType` - docx, xsl, pdf etc. `docx` in the example
  * `documentPath` - full path to the document inside Process Application Archive. Used as resource reference in BPMN. `templates/testTemplate1.docx` in the example
  * `code` - contains the same value as documentPath (`/templates/testTemplate1.docx`)
  * `documentName` - a name of the document file `testTemplate1.docx`
  * `outputNamePattern` - output file name, e.g. `testTemplate1-result.docx`
* Add a Service task `Generate report` to the diagram and point it to `com.artezio.bpm.reporting.yarg.YargDelegate` class (e.g. Delegate expression `${yargDelegate}` when using CDI). The task must be configured as follows:
  * Add input parameter named `template` of type `Text`. Enter template with full path, e.g. `templates/testTemplate1.docx` 
  * Add input parameter named `params` of type `Map`. Add all params that are required within Yarg template. The key specifies param name, while the value contains the parameter itself and may contain for example process variable reference: [`param1`: `${someCalculatedValue}`]. Each param will be available in the report for accessing by its key, e.g. param with key `param1` could be accessed from JSON report band query as `parameter=param1 $.someValue`
  * Add input parameter named `resultVariableName` of type `Text`. This points to a process variable which will be set with the generated report file of type `org.camunda.bpm.engine.variable.value.FileValue`
    
---

#### Using JUEL expressions

There is a feature to evaluate [JUEL] expressions inside templates

To evaluate an expression, put it with `#{}` brackets anywhere inside DOCX, DOC, XLSX or XLS file:

`#{priceStorage.getItemPrice(${item.id}) + 3}`

You can use band data in an expression as always: `#{${item.price} - ${defaults.defaultPrice}}`. Band data is evaluated before JUEL expression

If you need to use quotes (`"`) inside an expression, you may need to switch off smart quotes substitution in your editor. To do that in MS Word, open `File->Options->Proofing->AutoCorret Options` and turn off `Replace Straight quotes with Smart quotes` on each tab

---

#### Using conditional formatting in Yarg templates

There is a feature to display or hide data depending on dynamically calculated condition. This feature is supported for *doc, docx, xls, xlsx* input templates

Condition format:

> ### Note <br/>
> There's no need to put `#{} or ${} brackets around EL expression. Everything after IF is considered to be an EL expression.`

```
#{IF <Juel expression with Boolean result>} 
  <Displayed if expression is True> 
#{ELSE} 
  <Displayed if expression is False> 
#{ENDIF}
```
`#{ELSE}` block is optional

`#{IF} #{ENDIF}` blocks are mandatory


Juel expression is any java EL 3.0 expression. Static method invocations are not supported  

When writing conditionals inside **DOCX** templates, the entire IF-ELSE-ENDIF block must reside inside a *single paragraph*. DOC, XLS, XLSX templates have no special requirements

If you need to use quotes (`"`) inside an expression, you may need to switch off quotes substitution in your editor. To do that in MS Word, open `File->Options->Proofing->AutoCorret Options` and turn off `Replace Straight quotes with Smart quotes` on each tab   

You can use any references to Yarg band data inside Juel expressions and between IF-ELSE-ENDIF tags, as in the following example 

---
#### DOCX conditional template example
Output text depending on item.value using bean method getMinValue. Then output "true is equal to true"

The  `${}` expressions are Yarg data references. The `#{}` expressions are JUEL expressions
   
```

   --- paragraph begin ---

   #{IF bean.getMinValue() > bean2.calculate(${item.value}) }
       The item ${item.name} has value greater than min value which is #{bean.getMinValue()} 
   #{ELSE}
       The item ${item.name} has value less than min value which is #{bean.getMinValue()}
   #{ENDIF}
   
   --- paragraph end ---



   --- paragraph begin ---
   
   #{IF true == true} true is equal to true #{ENDIF}
   
   --- paragraph end ---
  
  
```

#### Running included test BPMN scenario with output files

Run YargDelegateTest.testGenerateTestReport. All generated files will be written into %TEMP% folder under subdirectory `generated-yarg-test-reports` 

[Yarg]: https://github.com/cuba-platform/yarg
[JUEL]: https://docs.oracle.com/javaee/5/tutorial/doc/bnahq.html
[OpenOffice]: https://www.openoffice.org/ru/
