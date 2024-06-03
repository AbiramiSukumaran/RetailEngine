# RetailEngine
Build a retail engine to answer user search queries.

## Make sure the AlloyDB objects are created before you begin with this step.

Schema and Data Ingestion:
Create a cluster and instance with cluster id “patent-cluster”, password “alloydb”, PostgreSQL 15 compatible and the region as “us-central1”, networking set to “default”. Set instance id to “patent-instance”. Click CREATE CLUSTER. You can create a table using the DDL statement below in the AlloyDB Studio:

CREATE TABLE apparels ( id BIGINT, category VARCHAR(100), sub_category VARCHAR(50), uri VARCHAR(200), image VARCHAR(100), content VARCHAR(2000), pdt_desc VARCHAR(5000), embedding vector(768) );

Now that the schema is defined, let’s go ahead and ingest data into the table:

Copy the INSERT DML scripts from this repo (https://github.com/AbiramiSukumaran/spanner-vertex-search/blob/main/data%20files/insert_script.sql) and run it from AlloyDB Studio Query Editor. This should insert 2906 records. If you want to test it with limited records, feel free to cut it down.

Enable Extensions:
For building this app, we will use the extensions pgvector and google_ml_integration. The pgvector extension allows you to store and search vector embeddings. The google_ml_integration extension provides functions you use to access Vertex AI prediction endpoints to get predictions in SQL. Enable these extensions by running the following DDLs:

CREATE EXTENSION vector;
CREATE EXTENSION google_ml_integration;

Grant Permission:
Run the below statement to grant execute on the “embedding” function:

GRANT EXECUTE ON FUNCTION embedding TO postgres;

Context:
We will now update the pdt_desc field with context data:

update apparels set pdt_desc = concat('This product category is: ', category, ' and sub_category is: ', sub_category, '. The description of the product is as follows: ', content, '. The product image is stored at: ', uri) where id is not null;

This DML creates a simple context summary using the information from all the fields available in the table and other dependencies (if any in your use case). For a more precise assortment of information and context creation, feel free to engineer the data in any way that you find meaningful for your business. 

Embeddings:
Now that the data and context are ready, we will run the below DML to convert the knowledge base into embeddings for the conversation assistance:

UPDATE apparels set embedding = embedding( 'textembedding-gecko@003', pdt_desc) where id > 2500 and id <= 3000;

We have used textembedding-gecko@003 from Vertex AI to convert the data into embeddings. If you have a custom embedding model, feel free to use that instead. Refer this to register and call remote AI models in AlloyDB: https://cloud.google.com/alloydb/docs/ai/model-endpoint-overview

## Java Cloud Functions

1. Go to Cloud Functions in Google Cloud Console to CREATE a new Cloud Function or use the link: https://console.cloud.google.com/functions/add. 

2. Provide Function Name “retail-engine” and choose Region as “us-central1”. Set Authentication to “Allow unauthenticated invocations” and click NEXT. Choose Java 11 as runtime and Inline Editor for the source code.

3. Copy the contents of the JAVA file and pom.xml file from this project and replace the ones in your new Google Cloud Java Cloud Function.

4. Change the PROJECT_ID placeholder in the JAVA file in this project.

5. Deploy the Cloud Function and test it from the TESTING tab of your newly deployed Cloud Functions (If there are deployment errors you can see them in the LOGS tab).
