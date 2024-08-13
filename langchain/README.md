## Command to deploy to GCF

```
# Detect your public IP: `curl ipecho.net/plain` and add to GCF

# Deploy the GCF
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
