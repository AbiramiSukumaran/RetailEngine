import functions_framework


class settings:
    project_id = "<<PROJECT_ID>>"
    region = "us-central1"
    cluster_name = "<<CLUSTER_NAME>>"
    instance_name = "<<INSTANCE_NAME>>"
    database_name = "<<DB_NAME>>"
    user = "<<USER_NAME>>"
    password = "<<PASSWORD>>"


import json


from langchain_google_alloydb_pg import (
    AlloyDBEngine,
    AlloyDBLoader,
)


@functions_framework.http
def hello_http(request):
    request_json = request.get_json()
    search_text = request_json.get("search")
    query = f"""
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
    """

    loader = AlloyDBLoader.create_sync(engine=getDbEngine(), query=query, format='JSON')

    documents = loader.load()
    ans = [
        {**json.loads(doc.page_content), **doc.metadata}
        for doc in documents
    ]
    return json.dumps(ans)


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
