# Using our demonstration app.

We've created a small demonstration screen that uses uniVocity to migrate data provided by the United States Department of Agriculture (USDA).

If you go to the [USDA website](http://www.ars.usda.gov/Services/docs.htm?docid=8964), you will find all different releases of their nutrient database. This information is on the public domain and can be downloaded for free, and is updated roughly on a yearly basis.

We will show how uniVocity can be used to import this data into a database and update to new releases of the USDA nutrient database, and then to migrate this data to a completely different database.

We downloaded the USDA ASCII files containing the SR25 and SR26 databases and created [a database](http://github.com/uniVocity/univocity-examples/tree/master/src/main/resources/source/db) to store this information. The USDA files are in [this zip file](data.zip).

This is the entity relationship diagram of the source database:  
![alt text](/home/jbax/Desktop/demo-app/source-diagram.png)

The [destination database](http://github.com/uniVocity/univocity-examples/tree/master/src/main/resources/destination/db) looks like this:
![alt text](/home/jbax/Desktop/demo-app/destination-diagram.png)

Start the application inside our release package, under the `/demonstration_app` folder.

The following window will be displayed:
![alt text](.src/main/resources/source/db/diagram.png)

When you start the application, two in-memory databases are created to store the information. On the left side can query the tables of the source database, and on the right side the tables of the destination database. At first all tables will be empty.

![alt text](.src/main/resources/source/db/diagram.png)

Initially, we need to insert all the data into our source database. In the list process above, select "skip nutrient data - load/update source database using SR25 files", then click the "execute process"

![alt text](select_loadSR25.png)

[This](http://github.com/uniVocity/univocity-examples/blob/master/src/main/java/com/univocity/app/etl/LoadSourceDatabase.java) is the code responsible for mapping the files into the source database:

```java

	DataIntegrationEngine engine = Univocity.getEngine(getEngineName());
	DataStoreMapping mapping = engine.map("data", "source");
	mapping.configurePersistenceDefaults().usingMetadata().deleteAbsent().updateModified().insertNewRows();
	mapping.autodetectMappings();
	engine.setMappingSequence("FD_GROUP", "FOOD_DES", "NUTR_DEF", "WEIGHT", "NUT_DATA");
``` 

At the end of this first mapping cycle, you finally have some data to play with. I created some queries to display some of this information in a more usable fashion.

![alt text](afterSR25.png)

So that was easy, and a competent programmer would be able to implement the same mapping code manually to load a database from these files and get this far in a few hours.

But now, let's map this database to the destination database. Select the process "Skip nutrient data - migrate from source database to destination database".

![alt text](select_migrate_do_dest.png)
 
The [mapping](http://github.com/uniVocity/univocity-examples/blob/master/src/main/java/com/univocity/app/etl/LoadSourceDatabase.java) for to execute this is:

```java
DataIntegrationEngine engine = Univocity.getEngine(getEngineName());
engine.addDatasetProducer(EngineScope.CYCLE, new FoodProcessor()).on("FOOD_DES", "Ndb_no", "Long_Desc");

DataStoreMapping mapping = engine.map("source", "destination");
mapping.configurePersistenceDefaults().usingMetadata().deleteAbsent().updateModified().insertNewRows();

EntityMapping map = mapping.map("food_names", "food_name");
map.identity().associate("name").toGeneratedId("id");
map.value().copy("name").to("description");

map = mapping.map("food_state_names", "food_state");
map.identity().associate("name").toGeneratedId("id");
map.value().copy("name").to("description");

map = mapping.map("food_name_details", "food");
map.identity().associate("food_code").toGeneratedId("id");
map.reference().using("name").referTo("food_names", "food_name").on("name_id").directly().onMismatch().abort();

map = mapping.map("food_state_details", "state_of_food");
map.identity().associate("food_code", "name", "order").to("food_id", "state_id", "sequence");
map.reference().using("food_code").referTo("food_name_details", "food").on("food_id");
map.reference().using("name").referTo("food_state_names", "food_state").on("state_id");

map = mapping.map("FD_GROUP", "food_group");
map.identity().associate("FdGrp_CD").toGeneratedId("id");
map.value().copy("FdGrp_Desc").to("description");

map = mapping.map("FOOD_DES", "food");
map.identity().associate("NDB_No").to("id");
map.reference().using("NDB_No").referTo("food_name_details", "food").on("id");
map.value().copy("CHO_Factor", "Fat_Factor", "Pro_Factor", "N_Factor")
		.to("carbohydrate_factor", "fat_factor", "protein_factor", "nitrogen_protein_factor");
map.persistence().usingMetadata().deleteDisabled().updateModified().updateNewRows();

map = mapping.map("FOOD_DES", "group_of_food");
map.identity().associate("NDB_No", "FdGrp_Cd").to("food_id", "group_id");
map.reference().using("NDB_No").referTo("food_name_details", "food").on("food_id");
map.reference().using("FdGrp_Cd").referTo("FD_GROUP", "food_group").on("group_id");

engine.addFunction(EngineScope.STATELESS, "normalize", new FunctionCall<String, String>() {
	@Override
	public String execute(String input) {
		return input == null ? null : input.trim().toLowerCase();
	}
});

map = mapping.map("WEIGHT", "weight");
map.identity().associate("Msre_Desc").toGeneratedId("id").readingWith("normalize");
map.value().copy("Msre_Desc").to("description").readingWith("normalize");
map.addOutputRowReader(new NoDuplicatesRowReader("description"));

map = mapping.map("WEIGHT", "weight_of_food");
map.identity().associate("NDB_No", "Msre_Desc", "Amount").to("food_id", "weight_id", "amount");
map.reference().using("NDB_No").referTo("food_name_details", "food").on("food_id");
map.reference().using("Msre_Desc").referTo("WEIGHT", "weight").on("weight_id").readingWith("normalize");
map.value().copy("Gm_Wgt").to("grams");
map.addOutputRowReader(new NoDuplicatesRowReader("weight_id", "food_id", "amount"));

map = mapping.map("NUTR_DEF", "nutrient");
map.identity().associate("Nutr_No").toGeneratedId("id");
map.value().copy("NutrDesc", "units", "tagname").to("description", "unit", "acronym");

map = mapping.map("NUT_DATA", "nutrient_of_food");
map.identity().associate("NDB_No", "Nutr_No").to("food_id", "nutrient_id");
map.reference().using("NDB_No").referTo("food_name_details", "food").on("food_id");
map.reference().using("Nutr_No").referTo("NUTR_DEF", "nutrient").on("nutrient_id");
map.value().copy("Nutr_Val").to("amount");

``` 

Now have a look at the destination database. All the information from the source has been migrated correctly. The same information, in a wildly different database! Take your time to select the tables and analyze the data there. We created some queries to help you visualize the data in both source and destination. Select "Foods and their weights" on both sides, then search for "butter, salted" in the search field at the bottom. You should see this:

![alt text](after_migrating.png)

Now, go ahead and and change some data in the source database. Select the table `food_des`, select the first row of the table (butter, salted). Now change it to something else, let's say: "bacon, fried", and execute the process again. You should now find "bacon, fried" on the destination using the search field again.

![alt text](migrated_bacon.png)

You will notice that if you change any record in the destination database, they won't be overriden (unless they were also modified in the source database, to prevent that, righ click and disable updates to the record). That's because these mappings were defined to generated metadata which allows uniVocity to "know" when to perform data updates.

Take your time to explore the demonstration app. Delete records, update, create new ones, upgrade the source database from the SR25 to the SR26 version, downgrade it, synchronize everything again. All your data changes are handled by uniVocity, in those two snippets of code displayed before. Nothing more, nothing less. Think about how much time you would need to hand-code an ETL process to do this. Using uniVocity, we created these processes in less than one day.



# Things we found out when upgrading from SR25 to SR26

If you open the `sr26upd` folder in our zip file, you will find update files to apply over the SR25 database. These files are provided by the USDA and we left them there to validate our own update process. When creating this example, we came across an interesting situation: Some of the update files provided by the USDA are incorrect. uniVocity detects a number of rows to insert and update that does not match what is in the update files for WEIGHT and NUT_DATA.

Look at the log: uniVocity performed 1208 insertions and 2566 updates on the WEIGHT table. However, the ADD_WGT file has 1206 rows to insert.

Here are the rows missing in the ADD_WGT file:
```
[14296, 2, .5, scoop (2 tbsp), 29, null, null]
[19078, 3, .5, oz Hersheys, 14.2, null, null]
```
And this row is missing in the CHG_WGT file:
```
[25014, 2, 1, bag, 87, null, null] - changed from SR25: [25014, 2, 1, bag, 87, 12, 2.428]
```

We don't think this is a critical error on the USDA part as this is not critical information. We doubt someone is going to lose money or even their lives over a missing ounce of Hersheys. But this error illustrates very clearly that  keeping data accurate is not that straightforward. It also demonstrates how uniVocity can help you keep your data intact and precise with almost no effort. With uniVocity you can do much, much more than what you just saw.

There are more errors in these updates files and we created [this test case](http://github.com/uniVocity/univocity-examples/blob/master/src/test/java/com/univocity/app/DataUpdateTest.java) (on github) to print out everything.

Think about it for a moment: how long would you need to create your own data synchronization process to keep updating your database consistently every time your client data changes? We created the "load source database process" in a few minutes, without any complication.

How long would you need to create the same process with the tools you currently have?

### uniVocity is the only piece of software available in the world that allows you to create complex data mapping operations with almost zero effort.
## Download it now!

