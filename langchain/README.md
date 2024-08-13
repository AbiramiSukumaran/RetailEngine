# Deploy Retail Shopping App to GCF

`main.py` contains the steps needed to deploy the Retail Shopping App.


**Unused files but used as references:**

 1. `data.csv` is used by `extra.py::setup()` function to ingest data into Alloy DB.
 2. `extra.py::hello_http_no_langchain()` function shows a non-langchain way (more complicated approach) to query the data.


## Command to deploy to GCF

```
# Replace placeholder variables, such as `<<PROJECT_ID>>`
gcloud functions deploy <<CLOUD_FUNCTION_NAME>> \
    --entry-point main.py \
    --runtime python312 \
    --trigger-http \
    --allow-unauthenticated

# Define endpoint
CLOUD_FUNCTIONS_ENDPOINT=https://us-central1-<<PROJECT_ID>>.cloudfunctions.net/<<CLOUD_FUNCTION_NAME>>
```

# Testing the endpoint

```
curl -s -X POST \
  $CLOUD_FUNCTIONS_ENDPOINT \
  -H 'Content-Type: application/json' \
  -d '{"search":"I want womens footwear, black color, with heels and strictly no leather"}' \
  | jq .
```
