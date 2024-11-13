package gcfv2;


import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.io.BufferedWriter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.cloud.vertexai.generativeai.preview.ResponseHandler;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import com.google.gson.Gson;


public class HelloHttpFunction implements HttpFunction {
 @Override
 public void service(HttpRequest request, HttpResponse response) throws Exception {
// Get the request body as a JSON object.
JsonObject requestJson = new Gson().fromJson(request.getReader(), JsonObject.class);
String searchText = requestJson.get("search").getAsString();
   BufferedWriter writer = response.getWriter();
   String result = "";
String query = "SELECT\n" +
 "  X.id,\n" +
 "  X.category,\n" +
 "  X.sub_category,\n" +
 "  X.uri,\n" +
 "  X.content,\n" +
 "  X.description,\n" +
 "  X.user_text,\n" +
 "  literature\n" +
"FROM (\n" +
 "  SELECT\n" +
 "    XYZ.id,\n" +
 "    XYZ.category,\n" +
 "    XYZ.sub_category,\n" +
 "    XYZ.uri,\n" +
 "    XYZ.content,\n" +
 "    XYZ.description,\n" +
 "    XYZ.user_text,\n" +
 "    json_array_elements( google_ml.predict_row( model_id => 'gemini-1.5',\n" +
 "        request_body => CONCAT('{\"contents\": [ { \"role\": \"user\", \"parts\": [ { \"text\": \"Read this user search text: ' || user_text || ' Compare it against the product inventory data set: ' || content || ' Return a response with 3 values: 1) MATCH: if the 2 contexts are at least 85% matching or not: YES or NO 2) PERCENTAGE: percentage of match, make sure that this percentage is accurate 3) DIFFERENCE: A clear short easy description of the difference between the 2 products. Remember if the user search text says that some attribute should not be there, and the record has it, it should be a NO match.\" } ] } ] }')::json))-> 'candidates' -> 0 -> 'content' -> 'parts' -> 0 -> 'text'\n" +
 " AS literature\n" +
 "        FROM (\n" +
 "          SELECT\n" +
 "            id,\n" +
 "            category,\n" +
 "            sub_category,\n" +
 "            uri,\n" +
 "            id || ' - ' || pdt_desc AS content,\n" +
 "            pdt_desc AS description,\n" +
 "            '" + searchText + "' AS user_text\n" +
 "          FROM\n" +
 "            apparels\n" +
 "          ORDER BY\n" +
 "            embedding <=> embedding('text-embedding-004',\n" +
 "              '" + searchText + "')::vector\n" +
 "          LIMIT\n" +
 "            5 ) AS XYZ) AS X\n" +
"      WHERE\n" +
 "        CAST(literature AS VARCHAR(500)) LIKE '%MATCH:%YES%';";
   HikariDataSource dataSource = AlloyDbJdbcConnector();
   JsonArray jsonArray = new JsonArray(); // Create a JSON array
try (Connection connection = dataSource.getConnection()) {
     try (PreparedStatement statement = connection.prepareStatement(query)) {
       ResultSet resultSet = statement.executeQuery();
         while (resultSet.next()) { // Loop through all results
                             JsonObject jsonObject = new JsonObject();
                   jsonObject.addProperty("id", resultSet.getString("id"));
                   jsonObject.addProperty("category", resultSet.getString("category"));
                   jsonObject.addProperty("sub_category", resultSet.getString("sub_category"));
                   jsonObject.addProperty("uri", resultSet.getString("uri"));
                   jsonObject.addProperty("description", resultSet.getString("description"));
                   jsonObject.addProperty("literature", resultSet.getString("literature"));
                   jsonArray.add(jsonObject);
        }
     }
      // Set the response content type and write the JSON array
           response.setContentType("application/json");
           writer.write(jsonArray.toString());
   
    }
 }
 public  HikariDataSource AlloyDbJdbcConnector() {
  HikariDataSource dataSource;
  String ALLOYDB_DB = "postgres";
  String ALLOYDB_USER = "postgres";
  String ALLOYDB_PASS = "alloydb";
  String ALLOYDB_INSTANCE_NAME = "projects/$PROJECT_ID/locations/us-central1/clusters/shopping-cluster/instances/shopping-instance";
   HikariConfig config = new HikariConfig();
   config.setJdbcUrl(String.format("jdbc:postgresql:///%s", ALLOYDB_DB));
   config.setUsername(ALLOYDB_USER); // e.g., "postgres"
   config.setPassword(ALLOYDB_PASS); // e.g., "secret-password"
   config.addDataSourceProperty("socketFactory", "com.google.cloud.alloydb.SocketFactory");
   config.addDataSourceProperty("alloydbInstanceName", ALLOYDB_INSTANCE_NAME);
   dataSource = new HikariDataSource(config);
   return dataSource;
}
}
