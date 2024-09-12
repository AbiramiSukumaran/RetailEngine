/* Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/

package cloudcode.helloworld;

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

public class HelloWorld implements HttpFunction {
  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {
// Get the request body as a JSON object.
 JsonObject requestJson = new Gson().fromJson(request.getReader(), JsonObject.class);
 String searchText = requestJson.get("search").getAsString();
//searchText = "A new Natural Language Processing related Machine Learning Model";
    BufferedWriter writer = response.getWriter();
    String result = "";
       String query = "select id, category, sub_category, uri, user_text, description, literature from ( SELECT id, user_text, description, category, sub_category, uri, ML_PREDICT_ROW('projects/<<PROJECT_ID>>/locations/us-central1/publishers/google/models/text-bison-32k',  json_build_object('instances', json_build_array(json_build_object( 'prompt', 'Read this user search text: ' || user_text || ' Compare it against the product record: ' || content || ' Return a response with 3 values: 1) MATCH: if the 2 products are at least 70% matching or not: YES or NO 2) PERCENTAGE: percentage of match 3) DIFFERENCE: A clear short easy decription of the difference between the 2 products. Remember if the user search text says that some attribute should not be there, and the product record has it, it should be a NO match.'  )  ),  'parameters', json_build_object( 'maxOutputTokens', 1024,  'topK', 40, 'topP', 0.8,  'temperature', 0.2 )) ) -> 'predictions' -> 0 -> 'content' AS literature FROM  (SELECT '" + searchText + "' as user_text, id, category, sub_category, uri, content, pdt_desc  as description FROM apparels ORDER BY embedding <=> embedding('textembedding-gecko@003', '" + searchText + "' )::vector LIMIT 25) as xyz ) as X where cast(literature as VARCHAR(500)) like '%MATCH:%YES%' ";
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
   String ALLOYDB_DB = "*****";
   String ALLOYDB_USER = "*****";
   String ALLOYDB_PASS = "*****";
   String ALLOYDB_INSTANCE_NAME = "projects/<<YOUR_PROJECT_ID>>/locations/us-central1/clusters/<<ALLOYDB_CLUSTER_NAME>>/instances/<<ALLOYDB_INSTANCE_NAME>>";
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
