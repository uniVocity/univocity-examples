![thumbnail](./images/uniVocity-api.png)

Welcome to uniVocity
====================
**[uniVocity](http://www.univocity.com)** is a data integration framework for Java that provides a fast and flexible foundation for the 
implementation of complex data mappings and transformations. uniVocity is free for non-commercial use and gives you much more power and flexibility
than a conventional ETL framework.
This tutorial covers the essential building blocks you can use to develop powerful data integration solutions.

@@TOC

## Installation ##


To install uniVocity, you need two artifacts: 

1. the [public API](http://github.com/uniVocity/univocity-api), which provides the essential interfaces and configuration options to configure data inputs,
   outputs, and their mappings. You must write your code against the interfaces provided by this API. 

2. our data integration engine implementation, which can be downloaded from our [website](www.univocity.com) or from our [maven](http://maven.apache.org) repository (http://artifacts.univocity.com). 

We split the API so your code can be totally isolated from our implementation code. Any new version of uniVocity will support the published API's 
so you can update uniVocity transparently without worrying about compilation errors and code rewrites.  

### Maven settings ###

If you use [Maven](http://maven.apache.org), you'll need to add an entry for our repository to your `pom.xml` in order to obtain the `univocity-[version]` jar.

```xml
    
    <repositories>
        <repository>
            <id>univocity-releases</id>
            <url>http://artifacts.univocity.com/release</url>
        </repository>
    </repositories>
```

These are the dependencies you need to include in your `pom.xml`:

```
    
    <dependencies>
    ...
        <dependency>
            <groupId>com.univocity</groupId>
            <artifactId>univocity</artifactId>
            <version>[version]</version>
            <type>jar</type>
        </dependency>
    
        <dependency>
            <groupId>com.univocity</groupId>
            <artifactId>univocity-api</artifactId>
            <version>1.0.1</version>
            <type>jar</type>
        </dependency>
    ...
    </dependencies>
    
```

**Note**: univocity-api is in the [Maven Central Repository](http://search.maven.org)

To get get access to uniVocity *snapshot* releases, add an additional `repository` entry to the `repositories` section of your `pom.xml`:

```xml
    
    <repositories>
        ...
        <repository>
            <id>univocity-releases</id>
            <url>http://artifacts.univocity.com/snapshot</url>
        </repository>
    </repositories>
    
```

### Obtaining a license ###

uniVocity is free for non-commercial use and can be used without a license. In this case, batch operations are disabled. 
To unleash the true power of uniVocity, and experience maximum performance, we suggest you to obtain a license file.

You can get a free 30-day trial license immediately by simply creating a license request for your computer and sending it to us. To create a license request, you can execute one of
the following classes from the `univocity-[version].jar`, as regular java applications:

 1. The graphical license request wizard: `com.univocity.LicenseRequestWizard` (if you have a graphical interface).
 2. The command-line license request script: `com.univocity.LicenseRequest` (if you want to execute from the command line)

Once the license request process starts, provide your details to generate a license request file. 
Send your license request to licenses@univocity.com and you will receive your license file shortly after.  

Once you receive your `license.zip` file, all you need to do is to place it in your classpath so `uniVocity` can validate it and start up. You can also place the license file anywhere in your computer and invoke `Univocity.setLicensePath("/path/to/your/license.zip");` before starting the data integration engine.

*You can find more information about licenses [here](http://www.univocity.com/pages/license-request)* 

## Introduction ##

### Background ###

uniVocity is essentially a data mapping framework built around the concept of data stores and their entities:

 * A *data entity* is an abstraction for any software component that provides data in tabular format, such as: database tables, CSV files, arrays of objects, etc.
   It must provide:
  * A sequence of fields names that define the information stored for each individual record. 
  * A means to retrieve records, update and delete them, or write new ones. The entity can support all these operations or 
   be just read-only or write-only.
 * A *data store* is an abstraction for any software component that provides and manages access to one or more data entities: databases, CSV file directories, 
   sets of files, custom Java objects, etc. It can provide these additional features:
  * queries: treated by uniVocity as a read-only data entity that is represented by a string. The string defines some data retrieval operation, and
   its behavior is determined by the data store implementation. Queries can accept parameters if required.
  * transactional behavior: the data store can rollback/commit any changes made to its data entities in case of errors.     

With these components, uniVocity lets you create complex data mappings that define how information should flow from one data entity to another.
That's all you need to know for now, so let's get started.

### The data input example ###

In our example, we are interested in synchronizing data of foods and groups of foods with another data store.

As the source data store, we will be using a stripped-down version of the nutrient database provided by the 
[U.S. Department of Agriculture](http://www.ars.usda.gov/ba/bhnrc/ndl).

We provide this data in a couple of files that you can find  [here](http://github.com/uniVocity/univocity-examples/tree/master/src/test/resources/examples/source_data/csv).
Some of the original data was modified to make the examples easier to read.

The tables we are interested in are: @@LINK(FD_GROUP.csv) for general
groups of food and @@LINK(FOOD_DES.csv) for individual food descriptions. 
These are stored in 2 CSV files with the following content:


@@INCLUDE_CONTENT(0, src/test/resources/examples/source_data/csv/FD_GROUP.csv)

And

@@INCLUDE_CONTENT(0, src/test/resources/examples/source_data/csv/FOOD_DES.csv)

### Configuring the data stores ###

To use these files as data entities in uniVocity, we need to configure a data store. uniVocity comes with its own
CSV data store implementation, so all you need to do is configure it:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Example.getCsvDataStore)

To make the following examples simple and easy to read, we will map the contents of these CSV files to a destination data store 
that contains fixed-width entities. We don't have these entities physically on disk so all data will be written to a `String`.
This will require a little bit of extra configuration to provide the essential information required by uniVocity. We will need to define the
entities, their names, their fields and which fields should be used as identifiers.

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Example.getFixedWidthDataStore)

### uniVocity engine initialization ###

Having the data stores configured, we can finally configure and initialize a data integration engine:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Example.initializeEngine)

In order to perform complex mappings and enable different data management strategies, uniVocity relies on a couple of metadata tables.
If you don't provide an explicit configuration for these tables in an @@LINK(EngineConfiguration) object, uniVocity will automatically start
an in-memory database to store this information. All metadata information in this in-memory database will be lost once the engine is stopped.

While the in-memory metadata is very convenient for testing and initial development purposes, it is probably not enough for most real-world applications.
Make sure your @@LINK(EngineConfiguration) provides access to these tables in a database of your preference, unless you just need basic persistence functionalities. 

## Essential building blocks ##

The following sections introduce the basic building blocks that allow you to use uniVocity to create powerful data mappings.

### Copying data from CSV to Fixed-width entities ###

With a data integration engine registered into uniVocity, we can obtain an instance of @@LINK(DataIntegrationEngine) and define data mappings:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial001Basics.example001SimpleCopy)

The first step is to map a source data store to a destination data store, and then map their data entities.

When establishing a mapping between entities, it is mandatory to define an identifier mapping. This mapping must ensure that both source and destination values
form a unique representation of each record (similarly to primary keys in a database). The identifier does not need to be a primary key of a database, and uniVocity
won't perform any validation against its contents. If the source data can contain duplicates, you can manage that and discard/transform any anomaly with the features
described in the following sections. The identifier values of both source and destination will have their `String` representation used in uniVocity metadata tables.
Therefore, you must ensure identifiers can be converted from/to `String` consistently. 
uniVocity enables the use of transformations that can be used to achieve this if required. 

Mappings can be auto-detected by default if field names match in both source in destination (or if you provide a @@LINK(NameMatcher)).
As this is not the case in the example, we must create the mappings between values manually. `value().copy("FdGrp_Desc").to("name")` does what it says: it copies
the value in "FdGrp_Desc" to "name".

Then, we can configure the appropriate persistence settings. This configuration determines what should happen to the destination data when a mapping cycle is executed.
In this example, we will store the identity values in uniVocity's metadata, and all values in the source entity will be deleted before inserting.

With the mappings properly defined, we can execute a data mapping cycle and see what data ended up being written in the `food_group` fixed-width entity :

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial001Basics/example001SimpleCopy)

In general, the steps taken so far are the first thing you will do when creating your own data mappings. But we expect your input data to
be much, much more intricate than that. uniVocity provides many tools for you to easily handle such intricacies. Let's explore some of them.


### Using row readers to manage rows ###

One of the most powerful tools in uniVocity is the @@LINK(RowReader). With it you can intercept rows as soon as they are loaded from the source,
then modify, log, perform calculations, etc, and then send them over to uniVocity to continue with the mapping process. Once uniVocity has a mapped row, and is ready
to send it to the destination entity, you can use a @@LINK(RowReader) to intercept and manipulate the row before it is written.
Finally, after the row is successfully persisted, uniVocity modifies the 
row if needed (typically, when generated values are produced by the destination entity upon insertion of new records), and exposes it to RowReaders again.

In the following example we use the same mapping as before, but now we attach a RowReader to the input for discarding unwanted rows.
We also attach a RowReader to the output for converting all food group names to uppercase before writing to the output:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial001Basics.example002RowReaders)

In this example we simply get the existing mapping, add our @@LINK(RowReader)s and execute the mapping cycle again. The action happens in @@LINK(RowReader)'s `processRow` method:
this method exposes the original input row and the mapped row (if any) that is going to be inserted into a destination entity. The @@LINK(RowMappingContext) is provided by
uniVocity so you can have more control over and information about the mapping in execution. The names of each mapped field, both in the input or output, 
can be obtained from this contextual object, as well as their positions in the input/output arrays. If you are manipulating a huge input, we recommend that you
acquire the positions of fields you are interested in manipulating using the `initialize()` method.    

After executing a data mapping cycle with these @@LINK(RowReader) implementations, the output will contain 2 rows (the first 3 rows in the input were skipped): 

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial001Basics/example002RowReaders)

Note that you can have multiple @@LINK(RowReader)s defined for each step of the process (input, output, persisted). In this case they will be executed sequentially and
changes made by the first @@LINK(RowReader) will reflect on the next. A row discarded by one @@LINK(RowReader) won't be passed on to the next. 
 
While powerful, @@LINK(RowReader)s are more suitable for mappings that require very specific operations. In short, they are not particularly easy to reuse,
especially when you just want to perform some common data cleanup or transformations on fields processed by different mappings.


### Functions ###

uniVocity engines allow you to provide functions that can be used during the execution of a data mapping cycle.
Functions are especially useful to transform input values, and to provide input to destination fields in data mappings.

As functions can perform operations that are potentially expensive (such as invoking a web service somewhere to get an account number in another system),
they are associated with a scope. The scope in uniVocity is bound to each individual data integration engine instance. 
The following scopes, defined in @@LINK(EngineScope), are supported:

1.	PERSISTENT: outlives the data integration engine (depends on user configuration)
2.	APPLICATION: values are retained and reused while the engine is active
3.	CYCLE: values are retained and reused while a mapping cycle is active
4.	MAPPING: values are retained and reused while an individual entity mapping is active
5.	STATELESS: no values are retained and reused.
 
When a function is invoked, its signature, arguments and result are stored into the scope with which it has been associated. A subsequent function invocation within the same scope,
with the same arguments, will not trigger the function. Instead, the result obtained previously will be returned. This will happen until the scope is left.
 
Getting back to our example, let's see how they can be put to work:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial001Basics.example003Functions)

As you can see, functions are added to the engine, and referred to in mappings by their name, without arguments.
This mapping uses 2 functions: `copy("FdGrp_Desc").to("name").readingWith("trim", "toLowerCase")`.Here the values in "FdGrp_Desc" 
will be transformed by the "trim" function, and the result will then be passed on to "toLowercase"

Also notice we changed the mapping's persistence settings with ` persistence().usingMetadata().deleteDisabled().updateDisabled().insertNewRows()`.
This means we won't be updating existing rows. (Remember what was done in the previous example? We had 2 rows with uppercase letters.) Although this is a new mapping
and the previous one was discarded, the metadata will be used to identify which rows NOT to update.  

The result of the execution of this new mapping will be:

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial001Basics/example003Functions)

The result shows the previous 2 rows in uppercase and 3 new, all in lower case and, as a result of the `toInteger` function, no leading zeros. 

### Reference mappings ###

Analogous to foreign keys in a database, uniVocity supports the concept of references to identifiers produced in other mappings. 

Now, we want to map the CSV entity `FOOD_DES` to the fixed-width entity `food`. The @@LINK(FOOD_DES.csv) file has the field `FdGrp_Cd`, which contains references to @@LINK(FD_GROUP.csv).

In the previous example, the identity mapping between `FD_GROUP` and `food_group` was declared as: `identity().associate("FdGrp_CD").to("id")`. 
We want to maintain this association on the destination data store, instead of simply copying from whatever is in the input. 

To do that, we need to give uniVocity the basic metadata it needs to be able to locate identifiers previously 
mapped, and potentially transformed, by previous mappings.

A reference mapping is declared like this: `reference().using("FdGrp_Cd").referTo("FD_GROUP", "food_group").on("group")`

1. The `.using("FdGrp_Cd")` part tells uniVocity to read the value of this field in `FOOD_DES` (the source entity)
2. `.referTo("FD_GROUP", "food_group")` tells uniVocity to read the metadata generated by a mapping from "FD_GROUP" to "food_group".
   It will use the values read from "FOOD_DES.FdGrp_Cd" to find the identifier previously persisted to "food_group"
3. `.on("group")` defines what field in the destination entity (in this case "food.group") should receive the identifier restored from the metadata. 

References that could not be matched will be set to null by default. You can also choose to discard the row entirely, use a placeholder or abort the mapping cycle entirely.

The following example demonstrates how @@LINK(FOOD_DES.csv) can be mapped to `food`:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial001Basics.example004ReferenceMapping)

The output of this mapping will be:

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial001Basics/example004ReferenceMapping)


### Intercepting engine lifecycle activities ###

Each time the data integration engine is executed, several lifecycle events take place:

 - A data mapping cycle is started

 - An entity mapping is started within the cycle

 - The entity mapping is finalized

 - The mapping cycle is finalized

 - The engine becomes ready to execute a new data mapping cycle

 - An engine shutdown process is started

 - The engine stops completely

To obtain notifications about one or more of such events when they happen, you can use an @@LINK(EngineLifecycleInterceptor). This allows you to
prepare for, or to perform cleanup after, each lifecycle event. uniVocity provides an @@LINK(EngineLifecycleContext) object to its interceptors,
so you can obtain more detailed information about the current state of the engine.

The following example prints out a few messages of an @@LINK(EngineLifecycleInterceptor) in action.

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial001Basics.example005LifecycleInterceptors)

The execution of this code will produce the following output: 

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial001Basics/example005LifecycleInterceptors)

### Map functions ###

`Maps` can be used as functions, where the input parameter of the function is used as a key to retrieve a value from the map. For example:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial001Basics.example006MapFunctions)

In the above code, we created a `Map` from group codes to their descriptions. By creating a mapping using `copy("FdGrp_CD").to("group").readingWith("getNameOfGroup")`, we
assign the values in `FdGrp_CD` as parameters of the map function `getNameOfGroup`. The function will then return a group name for each code, and this name will be
written to the destination field `group`. 

The execution of this code will produce the following output: 

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial001Basics/example006MapFunctions)


### Objects with functions ###

In many situations having functions in isolation is insufficient to handle more complex data mappings. There are cases when information must
processed in multiple steps, or the value of one or more functions must be determined from the state of an object. To handle such cases, uniVocity
lets you annotate multiple methods of your classes with the @@LINK(FunctionWrapper) annotation.

In the following example, we use an instance of the @@LINK(NameSplitter) class. 
It will be used split `String`s between commas and assign a numeric value to each unique `String`. The numeric values that represent each
component of the input `String` will then be concatenated with the pipe character. The `toCodes` method of this class is annotated with
`@FunctionWrapper(scope = EngineScope.APPLICATION)`, and uniVocity will create a function with it so it can be used as a function in mappings:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial001Basics.example007ObjectsWithFunctions)

In the example above, `engine.addFunctions(splitter)` will create a function named `toCodes` in the engine. It is just a wrapper around the original
method of the *splitter* instance. The mapping declaring `copy("Long_Desc").to("description").readingWith("toCodes")` will send the original
food descriptions to this *toCodes* function and the result will be written to the destination field *description*.

The output will be as follows:
	
@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial001Basics/example007ObjectsWithFunctions)

## Queries, more functions, and variables ##

uniVocity strives for convenience. We thought it would be convenient to use SQL to produce data from your entities. Your data store doesn't even need to be a database!
In the following example, we tell uniVocity to enable database-like operations in the CSV entities. In the
example class @@LINK(Tutorial002QueriesFunctionsAndVariables), we did that by configuring the data store width 
`enableDatabaseOperationsIn("FD_GROUP", "FOOD_DES")` and providing the length of each field in entities *FD_GROUP* and *FOOD_DES*.

When database operations are enabled for any entity, an in-memory database will be created automatically, and all data in these entities will be dumped
into in-memory tables. Here's an example of how you can take advantage of this feature to easily create data mappings:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial002QueriesFunctionsAndVariables.example001QueryMapping)

In this example, we want to map only the food groups that are actually used. To do that, we add a query to the engine using 
`engine.addQuery(EngineScope.CYCLE, "groupsInUse")`. This will create a query with the name *groupsInUse*, and its results will be reused within the mapping cycle: 
if multiple mappings depend on the data returned by *groupsInUse*, the SQL query won't be executed again until a new mapping cycle is started.

The `.onDataStore("csvDataStore")` indicates to which data store the query applies.

`.from`String`(" select ... ` is the query itself. You can also use queries from files and resources in your classpath. Notice the select statement has a label for each
selected column, and the labels match the fields names in the destination entity. We did that so we could use auto-detection, instead of mapping each field manually.

Finally `.returnDataset` tells the engine to return the query results as a @@LINK(Dataset). @@LINK(Dataset)s can be used as regular entities in any mapping.
In this case, the dataset will be produced with the result of the query, and its field names will match the labels of the select statement.

With the *groupsInUse* query properly built and initialized, an entity mapping is created using `map("{groupsInUse}", "food_group")`.
Notice the curly braces around the query name. These indicate the source entity is a function. We are effectively mapping a function to an entity.      

Lastly, `queryMapping.autodetectMappings()` will create mappings between fields with the same name. As the dataset produced by our query contains the
field names *id* and *name*, and the destination entity *food_group* contains fields with these names, the mappings will be automatically created. As the destination 
entity *food_group* was configured with *id* as its identifier, an identity mapping will be created for this field. 

After executing the mapping cycle, the output contains only the food groups that are actually used in @@LINK(FOOD_DES.csv):

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial002QueriesFunctionsAndVariables/example001QueryMapping)

### Using parameters and variables ###

Queries can also be parameterized and used as functions anywhere. The following example demonstrates how you can define and use them in conjunction with variables.

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial002QueriesFunctionsAndVariables.example002QueryWithParameters)

The first query, *findGroupCode*, contains the named parameter `:groupDescription`. The `.returnSingleValue` option mandates that just one value should be returned.
`.directly` indicates no function will be applied to the result of this query, and `.onErrorAbort` will make the query throw an exception in case an 
unexpected result is produced. In this case, an unexpected result would be more than one value, or no value at all, being produced.

The next query, *foodsOfGroup*, contains two named parameters: `:foodGroupCode` and `:foodDescription`. This query returns a @@LINK(Dataset).

With the queries defined, a very different entity mapping is created: `map("{foodsOfGroup(findGroupCode($groupName), $foodName)}", "food")`.
This mapping uses the *foodsOfGroup* function to produce a dataset and map it to the destination entity *food*.
However, *foodsOfGroup* takes two parameters: the code of a food group, and the description of a food. Instead of using a literal value, we chose
to use the *findGroupCode* function to fetch food group codes. The $ sign in `$groupName` indicates there is a variable with the name *groupName*.
When executing this mapping, uniVocity will read the value of the variable *groupName* and invoke the function *findGroupCode* with it. The result
will be passed on to the fist parameter of the function *foodsOfGroup*, and its second parameter will receive the value of the variable *foodName*.

In this example, the variables *groupName* and *foodName* are set using the commands `engine.setVariable("groupName", "Dairy%")` and
`engine.setVariable("foodName", "CHEESE%")`, before executing a mapping cycle.

> ** Important: ** when using parameterized functions/queries as source entities, the *signature* is used to generate the metadata entries, not the actual variable values.
> This may have implications when detecting data updates for the destination entity. For example, if your mapping is configured with the `deleteAbsent` persistence setting:
> 
> 1. Records will be mapped mapped using a set of variable values
> 2. On the next cycle, if the values of these variables changed, the resulting dataset may be different.
> 3. uniVocity will remove records previously mapped, that are not in the current dataset.   
> 
> If you don't want such behavior, then you may need to consider creating a duplicate entity mapping using literal values instead of variables.

There's also an interesting reference mapping in this example: `reference().using("{findGroupCode($groupName)}").referTo("{groupsInUse}", "food_group").on("group")`

This reference mapping uses the result of the function call `findGroupCode($groupName)` to obtain the code of a food group. The result is applied to uniVocity's metadata
to discover which identifier was used in the destination of mapping *{groupsInUse} -> food_group* (from the previous example). The identifier restored from uniVocity's
metadata will then be copied to food's *group* field.  

Finally, we set the variables *groupName* to "Dairy%" and *foodName* to "CHEESE%", execute a mapping cycle, then print the results.
Next, we set them again to "Baby Foods" and "%" respectively. The result of both mapping cycles is as follows:  

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial002QueriesFunctionsAndVariables/example002QueryWithParameters)

## Mapping between incompatible schemas ##

One of the most powerful features of uniVocity is probably how easy it is to map data between different schemas, consistently. To demonstrate how this works
we are going to map data in the @@LINK(FD_GROUP.csv) and @@LINK(FOOD_DES.csv) files to a peculiar database.
This database stores food information and their descriptions in multiple languages

The following entity-relationship diagram shows how its tables are associated:
![alt text](./images/diagram.png)

The database DDL scripts are located [here](http://github.com/uniVocity/univocity-examples/tree/master/src/test/resources/examples/new_schema). 

In this example, we will populate the following tables:

 * LOCALE - the languages supported in the database.
 * FOOD_GROUP - groups of foods (without their descriptions)
 * FOOD_GROUP_DETAILS - descriptions and locale dependent information of each FOOD_GROUP
 * FOOD_NAME - names of foods (without locale dependent information)
 * FOOD_NAME_DETAILS - descriptions and locale dependent information of each FOOD_NAME
 * FOOD - food information: name, proteins, energy, etc.
 * GROUP_OF_FOOD - join table between foods and different groups.

The data integration engine will now be initialized with a @@LINK(JdbcDataStoreConfiguration). First we need to create a configuration object for the database.
As uniVocity is able to query the JDBC data store to return all information it needs (tables, columns and primary keys), 
the configuration effort is fairly minimal:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/ExampleWithDatabase.getNewSchemaDataStore)

The @@LINK(CsvDataStoreConfiguration) remains the same as before. We just need to register the engine.

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/ExampleWithDatabase.initializeEngine)

With the engine ready, all we need to do is to configure the mappings and execute a mapping cycle:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial003SchemaMapping.example001SchemaMapping)

This example is not very different from what we've seen, but some novelties can be highlighted:

**Locale**

 * An entity mapping from "nowhere" to the destination entity *locale*: `mapping = dsMapping.map("locale")` maps values from expressions/variables
in the engine context only.
 * `identity().associate("{$locale}").toGeneratedId("id")` will associate the current value in the *locale* variable to a generated identifier of the *locale table* 

**Mapping to *food_group* and *food_group_details***
 * In `map("FD_GROUP", "food_group")`, there is only one mapping: `identity().associate("FdGrp_CD").toGeneratedId("id")`. When a new value for *FdGrp_CD* appears on 
   the input entity, uniVocity will insert an empty row into *food_group*. This will produce a generated identifier. This identifier is associated with the value in
   *FdGrp_Cd* using uniVocity's metadata.
   
 * Next, on `map("FD_GROUP", "food_group_details")`, the destination entity *food_group_details* is a table with a composite primary key: *id* is a foreign key that 
   references *food_group*, and *loc* is another foreign key that references to *locale*. To map this properly, we need to configure 3 field mappings:
   
  * `identity().associate("FdGrp_CD", "{$locale}").to("id", "loc")`: this will link the values of *FdGrp_CD* to *food_group_details.id*, and the current value 
     of variable *locale* to *food_group_details.loc*. The values won't be written to the destination, the reference mappings will obtain the correct references. 
  * `reference().using("FdGrp_CD").referTo("FD_GROUP", "food_group").on("id")`: using the values in *FdGrp_CD*, uniVocity will have to query its metadata to discover
     which IDs were generated in the mapping from *FD_GROUP* to *food_group*. It will obtain the correct generated ID's in *food_group* and copy them 
     to *food_group_details.id*.
  * `reference().using("{$locale}").referTo(null, "locale").on("loc")`: using the current value of variable *locale*, uniVocity will once again query its metadata to discover
     which ID was generated in the mapping to the *locale* table. As the mapping to *locale* does contain a source entity, we use *null* as the source entity. 
     This mapping will obtain the correct generated ID in the *locale* table and copy it to *food_group_details.loc*.

The remainder of this code is very similar and we hope it is easier to understand at this point. Here is how the migrated data should look like in the destination database:

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial003SchemaMapping/example001SchemaMapping)

## Input management ##

### Input sharing ###
  
 In the previous example, there are 4 mappings reading from the source entity *FOOD_DES*. Does it mean uniVocity will read from it 4 times?
 The answer is: no, *unless you want it to*. uniVocity detects there are 4 *consecutive* mappings reading from the same source. By default, it will create a single reading
 process to extract the values required by all 4 mappings, in one go, then reuse the data in each mapping. This may have implications when the number of rows in memory 
 is limited to be smaller than the input: if the input has 100 rows but 10 rows are to be kept in memory, uniVocity will load 10 rows, then execute each one of the 4 mappings;
 Then, the next 10 rows will be read and used to execute each mapping again. This will proceed until all rows from the input are read.
 
 If a row is discarded from the input in one mapping, it will still be available for the other mappings. Data modifications on the row won't be visible on other mappings. 
 
 If you need to process all rows before executing the subsequent mappings, disable input sharing with: `entityMapping.setInputSharingEnabled(false)`.
 
 Additionally, the input will only be shared when mappings using data from the same source entity are executed one after the other. If mapping 1 reads from FOOD_DES,
 mapping 2 from FD_GROUP, and mapping 3 from FOOD_DES again, then all rows of FOOD_DES will be read twice: first execute mapping 1, then again to execute mapping 3.     


### Data increments ###

You probably don't want to execute a data mapping cycle using ALL records of your source entities every time a few changes were made. With uniVocity, you can 
provide *data increments* to execute your mappings using limited datasets for each source entity. 

This example demonstrates how you can create a data increment and use it to execute your data mappings:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial003SchemaMapping.example002UpdateAgainstDataset)
  
In the example, we created a list of rows with some data and received a @@LINK(DatasetFactory) from @@LINK(Univocity). With this factory, you can easily create implementations of 
@@LINK(Dataset) using common collections such as `java.util.List` or `java.util.Map`. Our dataset is created with 
`factory.newDataset(rows, "FdGrp_CD", "FdGrp_CD", "FdGrp_Desc")`, returns a dataset with the given rows, using "FdGrp_CD" as the identifier
of these rows. The field names in this dataset are "FdGrp_CD" and "FdGrp_Desc". It's also important to note that this creates a dataset with field names
 that match the ones in entity *FD_GROUP*

A @@LINK(DataIncrement) is used to return datasets for each source entity used by the mappings you want to execute in a cycle. uniVocity will invoke its `getDataset()` method
with the names of a data store and one of its entities. That's all data increment do: return a dataset with updates for a given entity.   
  
Finally, we call `engine.executeCycle(increment)` to perform a mapping cycle using the data increment. 
After the cycle is executed, the destination entities *food_group* and *food_group_details* will have the following data:  
  
@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial003SchemaMapping/example002UpdateAgainstDataset)

### Update prevention ###

Using uniVocity metadata, you can prevent further modification on some records that have already been mapped to the destination. Simply create a @@LINK(Dataset) 
with identifiers of those records of the destination entity that should not be modified, then call `engine.disableUpdateOnRecords(entityName, dataset)`

To enable updates again, you can simply invoke `engine.enableUpdateOnAllRecords(entityName)`. You can also enable updates for a limited set of rows,
by using a dataset.

The following example demonstrates how this works:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial003SchemaMapping.example003UpdatePrevention)

First we enabled updates using `persistence().usingMetadata().deleteDisabled().updateModified().insertNewRows()`.
Then, a dataset with the identifiers of food group "Milk, eggs and stuff" is built.
Finally, `engine.disableUpdateOnRecords("food_group_details", dataset)` is called to prevent this food group being updated, and a mapping cycle is executed.

The original data of @@LINK(FD_GROUP.csv) will be read, and the food group "Bird meat" will be updated to "Poultry Products", but "Milk, eggs and stuff" won't be changed.

After enabling updates again with `engine.enableUpdateOnAllRecords("food_group_details")` and executing another mapping cycle, the row with identifier `0, 1` will be updated
from "Milk, eggs and stuff" to "Dairy and Egg Products".

The output of this example is: 

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial003SchemaMapping/example003UpdatePrevention)

### Dataset producers ###

In the example to map data between different schemas some tables were left out:
 
 * FOOD_STATE - descriptions of states applicable to any food (without locale dependent information), such as "salted", "cooked", "raw", etc
 * FOOD_STATE_DETAILS - descriptions and locale dependent information of each FOOD_STATE
 * STATE_OF_FOOD - join table between foods and different food states.
 
Now, we will map our data to these tables as well. In this case, each food description will be divided:
 * The first portion of text before a comma is a food name
 * Anything after that is a list of food states. Food states are separated by comma. 

This means that FOOD_NAME_DETAILS will now contain the food name without its state. Descriptions such as
*"cheese,blue"* and *"cheese,brick"* will generate a single *"cheese"* record in FOOD_NAME_DETAILS; Values *"blue"* and *"brick"* will become records of FOOD_STATE_DETAILS.

In the end, we will have FOOD with a reference to a FOOD_NAME, and a join table STATE_OF_FOOD with references to each FOOD_STATE of a particular FOOD. 
Review the [entity-relationship diagram](http://github.com/uniVocity/univocity-examples/tree/master/src/test/resources/examples/new_schema/diagram.png) again if you find this too confusing. 
We made it complex on purpose in order to demonstrate how far you can go with uniVocity.

In this example, a @@LINK(DatasetProducer) will process the descriptions in each @@LINK(FOOD_DES.csv) record and generate different datasets for different destination entities.

A @@LINK(DatasetProducer) is an abstract class that tells uniVocity which datasets it is able to generate from a data entity. Entity mappings can be created using these datasets
as the source. Once a mapping cycle is started, and the mapping that uses one of these datasets is executed, the @@LINK(DatasetProducer) will produce
the expected dataset.

In the mapping example (presented later), we associate an instance of @@LINK(FoodProcessor) to the *FOOD_DES* source entity. @@LINK(FoodProcessor) will generate its datasets using
data from fields *"Ndb_no"* and *"Long_Desc"*`:

@@INCLUDE_CLASS(/src/main/java/com/univocity/app/etl/FoodProcessor)

In the @@LINK(FoodProcessor) constructor, we declare the names of each dataset it produces.

The `processStarted()` method is invoked by uniVocity to prepare the dataset producer to process incoming rows.

After notifying the producer, uniVocity will start reading rows from the input entity, and invoke `processNext()` for each one. The @@LINK(FoodProcessor) splits the original food 
descriptions (in *"Long_Desc"*) into a food name and its states. The original food code (in *"Ndb_no"*) is also associated with each food name and state.

After all rows were processed, uniVocity will invoke the `getDataset` method to execute its mappings. In the case of our @@LINK(FoodProcessor), the datasets will contain:

 * food_names = a set of all unique food names
 * food_name_details = a list containing the food name associated with the original value of *"Ndb_no"*
 * food_state_names = a set of all unique food states
 * food_state_details = a list with the food state and its sequence, associated with the original value of *"Ndb_no"*

Finally. the mapping definition is as follows:

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial004Advanced.example001DataSetProducer)

As the destination tables depend on a locale, we created one for "American English" directly in the database and received its ID.
This ID set as an engine constant with `engine.setConstant("locale", localeId)`. 

Our @@LINK(FoodProcessor) is then associated with the FOOD_DES entity, and configured to read values from fields *Ndb_no* and *Long_Desc*:
 `engine.addDatasetProducer(EngineScope.CYCLE, new FoodProcessor()).on("FOOD_DES", "Ndb_no", "Long_Desc")` 

The mapping `dsMapping.map("food_names", "food_name")` uses the "food_names" dataset from @@LINK(FoodProcessor) to generate identifiers
in *food_name*. uniVocity metadata will have associations between each food name and these identifiers.

`dsMapping.map("food_names", "newSchema.food_name_details")` will copy the food names to "food_name_details", and create references
to the IDs of "food_name" (generated by the previous mapping).

Next, `dsMapping.map("<datasets>.food_name_details", "food")` creates new entries in the FOOD table. It inserts new FOOD records and their generated IDs
will be associated with the original food code. References to IDs of "food_name" are also mapped. Food states are mapped in a similar fashion. 


> Notice that "food_name_details" has been prepended with 
> "<datasets>". This is required to resolve an ambiguity, as "food_name_details" exist in multiple data stores. "<datasets>" is a reserved data store name for uniVocity datasets.

The last mapping, `dsMapping.map("FOOD_DES", "food")`,  reads the source entity @@LINK(FOOD_DES.csv) to load the food composition
information required to fully populate the "FOOD" entities. This is a special case, because as this is a new mapping, uniVocity will produce new rows.
However we *don't want to insert these new rows*: this is additional information we want to use in records previously created for 
"FOOD" (in `dsMapping.map("<datasets>.food_name_details", "food")`).

For this to happen, we have to tell uniVocity to use these new rows for updating. This is done with the `.updateNewRows()` insert option. Each row produced by this mapping
will be used to update rows with matching identifiers.

The result of this mapping will be a bit harder to read as the information of a single CSV file got spread into 6 tables. We will print the contents of each table and
use a query to reconstruct the information. This way we can confirm whether everything got mapped correctly: 
 
@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial004Advanced/example001DataSetProducer)

## Advanced ##

### Persistent scope ###

uniVocity supports the concept of a persistent scope. This is a scope whose state is intended to outlive the data integration engine. 
To use a persistent scope, you are expected to provide an implementation of @@LINK(ScopeStorageProvider) in an @@LINK(EngineConfiguration) object.
The storage provider is the interface that is used by uniVocity store and retrieve values added to the persistent scope. 
The storage is fully under your control, and you can choose whether to use files, a database, a distributed cache or anything else you need. 

We have created an example method `example002PersistentScope` in @@LINK(Tutorial004Advanced) that
demonstrates how the persistent scope works.

### Custom entities ###

uniVocity provides some basic data stores and entities but it also allows you to create your own. We created (and documented) examples exploring 
the full set of features you can implement with custom entities [here](http://github.com/uniVocity/univocity-examples/tree/master/src/test/java/com/univocity/examples/custom). These entities are based on in-memory data
stored in lists. You will want to implement your custom entities to process file formats or other structures not natively supported by uniVocity.
The examples we provided include primitive transactions and custom `String`-based queries so that you can explore and quickly adapt to your own needs.

To keep it short, to use your own custom entities with uniVocity, you need:

1. A configuration class for your data store, which extends @@LINK(DataStoreConfiguration)

2. An implementation of @@LINK(CustomDataStore)

3. An implementation of @@LINK(CustomDataStoreFactory). This class receives your data store configuration and uses it to create an instance of your custom data store.

4. At the very least, an implementation of @@LINK(CustomReadableEntity) (for read-only entities). Your data store should be able to initialize and return instances of these entities.

5. (optional) If your entities must also be written to, implement @@LINK(CustomDataEntity). You don't need to support all data modification operations; 
Simply return null and uniVocity will handle only the operations you implemented. Even with custom entities, you can enable database operations and load your data
to uniVocity's in-memory database. If that is the case, you can implement only the `deleteAll()` and `write()` methods: at the end of a transaction, uniVocity will
automatically invoke these methods and dump all contents of its in-memory database into your custom entity. This way you won't need to implement the `update()` and `delete()`
operations, which can be tricky in structures such as text files.

6. (optional) If you want to be able to execute some form of querying against your data store, implement @@LINK(CustomQuery). A query does not need to be a SQL statement. It can
be anything in a `String` that has a meaning in the context of your data store. 

The @@LINK(Tutorial005CustomEntities) puts our examples of custom entities to use,
and demonstrates how uniVocity simply isolates you from major intricacies. Please explore and let us know what you think of these examples.

### Advanced settings for JDBC entities ###

In many situations, your database and development environment will have limitations and special cases. 
uniVocity tries to provide options so you can get around them. Two of the most common situations are:

1. The need to handle SQL in a particular way, such as managing logical exclusions.
 
2. Not having the ideal support from JDBC drivers to enable batch insertions when generated key retrieval is needed.

uniVocity's JDBC entities provide configurations that allow you to handle such situations cleanly.  

For example, suppose the *locale* table supports the concept of "logical exclusion", where a "deleted" column is set to 'Y' to delete records. In such situation, the SQL
produced by uniVocity must take into account this restriction when selecting/deleting from this table. As we can't predict what your requirement will be in a generic
way, uniVocity delegates the control to you, through a @@LINK(SqlProducer), so you can define how SQL statements should be generated. 
For example, to properly select rows from *locale*, with logical exclusion, we can configure it to use the following class:

@@INCLUDE_CLASS(/src/test/java/com/univocity/examples/LogicalExclusionSelect)

Additionally, suppose the JDBC driver, or the database itself, does not support batch inserts when generated keys are to be returned. In this case, you can configure
the JDBC entity to use a custom generate key retrieval strategy for enabling batch insert operations. uniVocity's JDBC entities can be configured to use the
following strategies:

 * Fetch from statement: the default option. Executes insert operations one by one, and retrieves the generated key after each insert. This is the slowest strategy
   and you should consider the other strategies to execute inserts in batch.
    
 * Fetch from statement, using batch: Depends on your JDBC driver. Some drivers support batch insert execution and the `java.sql.Statement` will return all generated
   keys in a java.sql.ResultSet. If you can, use this option. If you can't, then read on.
   
 * Fetch from query: Executes a query to fetch the generated keys after a batch insertion. This is highly unsafe if there are multiple applications writing 
   to the table at the same time. It can only work reliably if you are sure no other modifications are occurring to the same table, or if your transaction
   isolation level is set to `SERIALIZABLE`. If you choose this option, make sure to test it carefully.
 
 * Use a `String` column: uniVocity will generate special `String` values to add to a column of your table. These values will then be used to query the table in order
   to obtain generated keys after a batch. This requires you to create an extra `VARCHAR(70)` column to your table.
   
 * Use a pair of numeric columns: similar to the `String` column, uniVocity will generate special integer values to add to a couple of columns of your table. 
   These values are used to query your table and restore the generated keys after a batch of inserts. This requires you to add 2 extra `INTEGER` columns to your table.   
		
		
The following example demonstrates how we modify the "locale" table to use a @@LINK(SqlProducer) and the numeric column strategy to fetch generated keys after an insert batch.

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial006JdbcConfigurationOptions.getNewSchemaDataStore)

In this snippet, we modify the original "locale" table by adding a "deleted" column for logical exclusion. We also add 2 integer columns to the table in order to enable
inserts in batch with generated key retrieval.

In order to properly read from "locale" and bypass rows marked with `deleted='Y'`, we provide our implementation of @@LINK(SqlProducer): 
`config.setSqlProducer(new LogicalExclusionSelect())`. This will ensure that when reading from "locale", only rows with "deleted='N'" will be selected.

The `config.retrieveGeneratedKeysUsingNumericColumns("process_id", "id_tracker", "id")` configuration assigns the two integer columns to track generated keys and 
return the values in the "id" column after a batch insert.

Finally, we can define some mappings to test how this will work

@@INCLUDE_METHOD(/src/test/java/com/univocity/examples/Tutorial006JdbcConfigurationOptions.example001BatchInsertAndGeneratedKeyRetrieval) 

In this example we create a dataset of locales and use it as the source to insert values into "locale".
After executing this initial mapping, we perform a logical exclusion on locales 'en_US', 'en_AU' and 'en_GB'.
Finally, we create the `generatedLocaleIds` dataset. This dataset will receive data from the "locale" table.

After executing this mapping, you should see the following messages in the log: 

> "Preparing to execute SQL statement: insert into locale (ACRONYM, DESCRIPTION, PROCESS_ID, ID_TRACKER) values (?,?,?,?)"
> "Preparing batches of 10000 rows. Extracting generated keys."
> ...
> "Generated keys extracted from custom tracker"
> ...
> Preparing to execute query select ID,ACRONYM from locale where deleted = 'N'

As you can see in the logs, the insert operation is being executed in a batch, and all generated keys are being retrieved using a custom tracker. Additionally, the
select statement used to fetch data from "locale" came from the implementation in @@LINK(LogicalExclusionSelect).

The output of this mapping will display all rows in the "locale" table (including the ones where deleted = 'Y'), 
and the values in the map wrapped by the `generatedLocaleIds` dataset. Note no locales with the deleted flag set to 'Y' are part of the map.

@@INCLUDE_CONTENT(0, /src/test/resources/examples/expectedOutputs/Tutorial006JdbcConfigurationOptions/example001BatchInsertAndGeneratedKeyRetrieval)


## Project Roadmap ##

While you can do a lot with uniVocity already, we think there is a lot of room for improvements that will make your life even easier.

We are just getting our feet wet! These some of the powerful new features in the pipeline!

Note: the following feature plan is not set in stone and items/dates can be moved around. Send your suggestions to `dev@univocity.com`


Version 1.1 (December, 2014)

	- more options for storing and manipulating metadata
	
	- introduction of variable values in metadata (no need to duplicate entity mappings with different literals)

	- introduce support for a name dictionary so that mappings can remain unchanged if entity or field names are not consistent in different environments, or if the original names were modified.

	- add support for qualified objects with functions. This way different instances of the same class can be used in the same engine.

	- better support for in-memory database generation, based on field types (no more Varchar for everything)

	- easier modification of existing mappings.

	- expressions (based on queries or functions) that produce data sets should be used as input entities in mappings

	- provide convenience RowReader/FunctionCall implementations by default for easier manipulation of rows and values
	
	- API support for additional data store types (planned: POJO and JSON)
	
Version 1.2 (April, 2015)

	- introduce support for Java 8 and provide an exclusive API that supports its new features. 

	- reverse mappings (automatically generate a mapping from destination to source, based on existing mappings and their metadata)

	- introduce auto-detection features for field types, lengths, formats, etc
	
	- support event-driven data updates in the API
	
	- API support for additional data store types (planned: XML, XBRL - we may introduce this in 1.1)
	
Version 1.3 (August, 2015)
	
	- JMX monitoring support and profiling tools
	
	- eclipse plug-in with code generation and easier configuration features
	
	- introduce support for data overflow to disk during the execution of data mappings
	
	- support for distributed transactions (JTA)
	 
	- API support for additional data store types (planned: JPA entities, Annotation-driven mappings)
	
