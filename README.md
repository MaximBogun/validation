# Java Validation Framework

[![Build Status](https://travis-ci.org/imsweb/validation.svg?branch=master)](https://travis-ci.org/imsweb/validation)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.imsweb/validation/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.imsweb/validation)

This framework allows edits to be defined in [Groovy](http://www.groovy-lang.org/) and to be executed on various data types.

## Features

* Edits are written in Groovy, a rich java-based scripting language.
* Large tables can be provided to the edits as contexts and shared among several edits.
* Edits can be loaded from an XML file, or defined programmatically.
* Any type of data can be validated; it just needs to implement the *Validatable* interface.
* The execution of edits is thread safe and the engine can be used in a heavily threaded application.
* Edits can be dynamically added, modified or removed in the engine.
* The engine supports an edit testing framework with unit tests written in Groovy as well.

## Download

The library is available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.imsweb%22%20AND%20a%3A%22validation%22).

To include it to your Maven or Gradle project, use the group ID `com.imsweb` and the artifact ID `validation`.

You can check out the [release page](https://github.com/imsweb/validation/releases) for a list of the releases and their changes.

## Core concepts

**ValidationEngine**

This is the class responsible for executing the edits. It needs to be initialized before that can happen.

**XmlValidatorFactory**

Provides utility methods for reading/writing edits XML files.

**Validator**

A logical grouping of edits (for example, the SEER edits, or the NAACCR edits). This is the entities used to initialize the validation engine.

**Rule**

Edits are called rules in this framework.

**Context**

Validators can also contain contexts; those are usually large data structures (list, maps, etc...) that are accessed by more than one edit. 
Edits can reference contexts using the prefix *"Context."*.

**Validatable**

An interface used to tell the engine how to execute the edits on specific data types. This allows very different types
(like a NAACCR line, a Java tumor object or a record from a data entry form) to be wrapped into a validatable and handled by the framework.

**ValidatorServices**

Some services are made available to the edits (like accessing a lookup, or a configuration variable); different applications
provide those features differently, therefore the services need to be overridden if the default behavior is not the one needed.

**ValidatorContextFunctions**

The methods from this class are made available to the edits; they can be called using the prefix *"Function."*.
The default implementation provides very basic methods but it can be initialized with a more complex implementation if needed.

If the edits have been translated from a Genedits metafile, the MetafileContextFunctions class should be used instead.
The initialization of that class requires an instance of the following staging algorithm:
- CS (https://github.com/imsweb/staging-algorithm-cs)

If the edits need to access staging information (to execute SEER edits for example), the StagingContextFunctions class should be used for initialization.
The initialization of that class requires an instance of the following staging algorithms:
- CS (https://github.com/imsweb/staging-algorithm-cs)
- TNM (https://github.com/imsweb/staging-algorithm-tnm)
- EOD (https://github.com/imsweb/staging-algorithm-eod-public)

## Usage

### Reading a file of edits

Here is an example of a very simplified XML file:

```xml
<validator id="my-edits">
    <rules>
        <rule id="my-edit" java-path="record">
            <expression>return record.primarySite != 'C809'</expression>
            <message>Primary Site cannot be C809.</message>
        </rule>
    </rules>
</validator>
```

And here is the code that can be used to initialize the validation engine from that file:

```java
File file = new File("my-edits.xml")
Validator v = XmlValidatorFactory.loadValidatorFromXml(file);
ValidationEngine.initialize(v);
```

### Creating an edit programmatically

This example shows how to initialize the validation engine from edits created within the code.

```java
// create the rule
Rule r = new Rule();
r.setRuleId(ValidatorServices.getInstance().getNextRuleSequence());
r.setId("my-edit");
r.setJavaPath("record");
r.setMessage("Primary Site cannot be C809.");
r.setExpression("return record.primarySite != 'C809'");

// create the validator (a wrapper for all the rules that belong together)
Validator v = new Validator();
v.setValidatorId(ValidatorServices.getInstance().getNextValidatorSequence())
v.setId("my-edits");
v.getRules().add(r);
r.setValidator(v);

// initialize the engine
ValidationEngine.initialize(v);
```

### Executing edits on a data file

This example shows how to validate a data file and print the edit failures; it uses the [layout](https://github.com/imsweb/layout)
framework to read a NAACCR file and translate it into a map of properties that the validation engine can handle.

```java
File dataFile = new File("my-data.txd.gz");
Layout layout = LayoutFactory.getLayout(LayoutFactory.LAYOUT_ID_NAACCR_16_ABSTRACT);
for (<Map<String, String> rec : (RecordLayout)layout.readAllRecords(dataFile)) {

    // this is how the engine knows how to validate the provided object
    Validatable validatable = new SimpleNaaccrLinesValidatable(rec)

    // go through the failures and display them
    Collection<RuleFailure> failures = ValidationEngine.validate(validatable);
    for (RuleFailure failure : failures)
        System.out.println(failure.getMessage());
}
```

## Optimizing loading an executing edits

Several mechanisms are in place to speed up the initialization and the execution of the edits; most of those mechanisms are turned OFF by default.

### Speed up the initialization by enabling multi-threaded parsing

Groovy edits need to be parsed to be validated and to gather the used properties. That step can be slow for big edits, 
but it can be optimized by enabling multi-threaded parsing:
```java
XmlValidatorFactory.enableMultiThreadedParsing(2);
```
The parameter is the number of threads to use; a value of 2 will usually work well for this mechanism. The default is 1.

### Speed up the initialization by enabling multi-threaded compilation

Groovy edits need to be compiled before being executed by the engine. Again, that step can be slow for big edits, 
but it can be optimized by enabling multi-threaded compilation:
```java
ValidationEngine.enableMultiThreadedCompilation(4);
```
The parameter is the number of threads to use; a value of 4 will usually work well for this mechanism although it depends on the available resources. The default is 1.

### Speed up the initialization by disabling the re-alignment of the expressions and descriptions

By default the engine will re-align the expressions and descriptions so they look nice in an editor. If you do not intend to display the expressions 
and/or descriptions, you can turn that feature off: 

```java
XmlValidatorFactory.disableRealignment();
```
By default the feature is ON.

### Speed up the initialization and exuction by using pre-compiled edits

The engine supports registering pre-compiled edits; those edits will completely bypass the parsing and compilation steps. The edits will also need to be strongly typed in their 
syntax, allowing them to run much faster than regular Groovy edits.

Pre-compiled edits is an advanced features; the engine supports it by default but creating the edits is much more work than maintaining them in an XML file. 
See the "runtime" package for more information, in particular the RuntimeUtils class.
## About SEER

This library was developed through the [SEER](http://seer.cancer.gov/) program.

The Surveillance, Epidemiology and End Results program is a premier source for cancer statistics in the United States.
The SEER program collects information on incidence, prevalence and survival from specific geographic areas representing
a large portion of the US population and reports on all these data plus cancer mortality data for the entire country.