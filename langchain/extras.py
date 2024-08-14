class settings:
    project_id = "<<PROJECT_ID>>"
    region = "us-central1"
    cluster_name = "<<CLUSTER_NAME>>"
    instance_name = "<<INSTANCE_NAME>>"
    database_name = "<<DB_NAME>>"
    table_name = "<<TABLE_NAME>>"
    user = "<<USER_NAME>>"
    password = "<<PASSWORD>>"


from google.cloud.alloydb.connector import Connector, IPTypes
import json
import sqlalchemy


from langchain_google_alloydb_pg import (
    AlloyDBEngine,
    Column,
    AlloyDBVectorStore,
)


#### Connect to Alloy DB
def getConn():
    connector = Connector()
    connection_string = f"projects/{settings.project_id}/locations/{settings.region}/clusters/{settings.cluster_name}/instances/{settings.instance_name}"
    conn = connector.connect(
        connection_string,
        "pg8000",
        user=settings.user,
        password=settings.password,
        db=settings.database_name,
        ip_type=IPTypes.PUBLIC,
    )
    return conn


def getDBConn():
    pool = sqlalchemy.create_engine("postgresql+pg8000://", creator=getConn)
    return pool


def hello_http_no_langchain(request):
    request_json = request.get_json()
    search_text = request_json.get("search")
    debug_text = request_json.get("log_level", "NO_DEBUG")

    query = sqlalchemy.text(
        f"""
    SELECT CAST(id AS VARCHAR) AS id, category, sub_category, uri, user_text, description, LLM_RESPONSE
    from (
        SELECT id, user_text, description, category, sub_category, uri,
        ML_PREDICT_ROW(
            'projects/<<PROJECT_ID>>/locations/us-central1/publishers/google/models/text-bison-32k@002',
            json_build_object(
                'instances', json_build_array(
                    json_build_object(
                        'prompt', 'Read this user search text: ' || user_text || ' Compare it against the product record: ' || content || ' Return a response with 3 values: 1) MATCH: if the 2 products are at least 85% matching or not: YES or NO 2) PERCENTAGE: percentage of match, make sure that this percentage is accurate 3) DIFFERENCE: A clear short easy description of the difference between the 2 products. Remember if the user search text says that some attribute should not be there, and the product record has it, it should be a NO match.'
                    )
                ),
                'parameters', json_build_object(
                    'maxOutputTokens', 1024,
                    'topK', 40,
                    'topP', 0.8,
                    'temperature', 0.2
                )
            )
        ) -> 'predictions' -> 0 -> 'content' AS LLM_RESPONSE
        FROM  (
        SELECT '{search_text}' user_text, id, category, sub_category, uri, content, pdt_desc  as description FROM apparels
        ORDER BY embedding <=> embedding('textembedding-gecko@003', '{search_text}')::vector LIMIT 25) as xyz
        ) as X
        WHERE cast(LLM_RESPONSE as VARCHAR(500)) like '%MATCH:%YES%' limit 10;
    """,
    )

    pool = getDBConn()

    ansString = '[{"No products matched the search criteria"}]'
    with pool.connect() as db_conn:
        result = db_conn.execute(query)
        rows = result.fetchall()
        column_names = result.keys()
        result_list = [dict(zip(column_names, row)) for row in rows]

        ansString = json.dumps(result_list)
        db_conn.close()
    return ansString


def getDbEngine():
    engine = AlloyDBEngine.from_instance(
        project_id=settings.project_id,
        instance=settings.instance_name,
        region=settings.region,
        cluster=settings.cluster_name,
        database=settings.database_name,
        user=settings.user,
        password=settings.password,
    )
    return engine


def getEmbeddingService():
    from langchain_google_vertexai import VertexAIEmbeddings

    embeddings_service = VertexAIEmbeddings(
        model_name="textembedding-gecko-multilingual@001", project=settings.project_id
    )
    return embeddings_service


def getVectorStore():
    engine = getDbEngine()
    engine.init_vectorstore_table(
        table_name=settings.table_name,
        vector_size=768,
        content_column="pdt_desc",
        embedding_column="embedding",
        metadata_columns=[
            Column("ids", "INTEGER", nullable=False),
            Column("contents", "VARCHAR", nullable=False),
            Column("category", "VARCHAR", nullable=False),
            Column("sub_category", "VARCHAR", nullable=False),
            Column("uri", "VARCHAR", nullable=False),
            Column("image", "VARCHAR", nullable=False),
        ],
        overwrite_existing=True,  # Enabling this will recreate the table if exists.
    )
    _vector_store = AlloyDBVectorStore.create_sync(
        engine=engine,
        embedding_service=getEmbeddingService(),
        table_name=settings.table_name,
        content_column="pdt_desc",
        embedding_column="embedding",
        metadata_columns=[
            "ids",
            "contents",
            "category",
            "sub_category",
            "uri",
            "image",
        ],
    )
    return _vector_store


def setup() -> None:
    # Import necessary libraries
    import pandas as pd
    from langchain_core.documents import Document

    # Get the vector store instance
    vector_store = getVectorStore()

    # Read the CSV file into a DataFrame
    df = pd.read_csv("data.csv", header=0)

    # Initialize an empty list to hold Document objects
    documents = []

    # Iterate over each row in the DataFrame
    for _, row in df.iterrows():
        # Create a metadata dictionary for the current row
        metadata = {
            "ids": row["ids"],
            "category": row["category"],
            "sub_category": row["sub_category"],
            "uri": row["uri"],
            "image": row["image"],
            "contents": row["contents"],
        }

        # Create a text description for the current row
        txt = f"""This product category is: '{row['category']}' and sub_category is: '{row['sub_category']}'. The description of the product is as follows: '{row['contents']}'. The product image is stored at: {row['uri']}."""

        # Create a Document object with the text and metadata, and add it to the documents list
        documents.append(Document(page_content=txt, metadata=metadata))

    # Add all the Document objects to the vector store
    vector_store.add_documents(documents)
