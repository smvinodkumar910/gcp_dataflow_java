
package com.mycloud.dataflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.exasol.jdbc.EXADriver;

import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.mycloud.configuration.AppProperties;
import com.mycloud.configuration.SchemaLoad;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.JsonToRow;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.Row;

import org.json.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ApiToDb {

  private static Logger logger = LoggerFactory.getLogger(ApiToDb.class);

  public List<String> getApiData(String url) {
    // Create object to be retured
    List<String> list = new ArrayList<String>();
    try {
      // Create HTTP Client to send get request
      HttpClient httpClient = HttpClientBuilder.create().build();
      HttpGet getRequest = new HttpGet(url);
      getRequest.addHeader("accept", "application/json");

      logger.info("Sending get Request to url: "+url);
      HttpResponse response = httpClient.execute(getRequest);

      if (response.getStatusLine().getStatusCode() != 200) {
        logger.error("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        throw new RuntimeException();
      }

      BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
      StringBuffer jsonoutput = new StringBuffer();
      String output = null;

      while ((output = br.readLine()) != null) {
        jsonoutput.append(output);
      }

      logger.info("Converting API response into List of String");
      JSONObject obj = new JSONObject(jsonoutput.toString());
      JSONArray array = obj.getJSONArray("data");
      for (int i = 0; i < array.length(); i++) {
        list.add(array.getJSONObject(i).toString());
      }
      
    } catch (ClientProtocolException e) {
      e.printStackTrace();

    } catch (IOException e) {
      e.printStackTrace();
    }
    logger.info("Returning API Response as ArrayList of String");
    return list;
  }

  public static void main(String[] args) {
    
    // Read application properties
    AppProperties appProperties = new AppProperties();

    // Get Source API Details
    Map<String, String> apidtl = appProperties.getProperty("sourceApi");
    // Get BQ Table details
    Map<String, String> gcpdtl = appProperties.getProperty("gcp");

    //Get Data schema properties
    SchemaLoad schemaLoad = new SchemaLoad(gcpdtl.get("tableid"));
    ApiToBq apiToBq = new ApiToBq();

    // Currently hard-code the variables, this can be passed into as parameters
    // String tempLocationPath = "gs://mycloud-proj-bucket/staging";
    boolean isStreaming = true;
    TableReference tableRef = new TableReference();
    // Replace this with your own GCP project id
    tableRef.setProjectId(gcpdtl.get("projectid"));
    tableRef.setDatasetId(gcpdtl.get("datasetid"));
    tableRef.setTableId(gcpdtl.get("tableid"));

    PipelineOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().create();
    // This is required for BigQuery
    options.setTempLocation(gcpdtl.get("tempLocationPath"));
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
    options.setJobName("apitobq-"+timeStamp);

    Pipeline p = Pipeline.create(options);
    logger.info("Creating pipeline");
    p.apply("Getting API Data", Create.of(apiToBq.getApiData(apidtl.get("URL"))))
        .apply("JSON To Beam Rows",
            JsonToRow.withSchema(schemaLoad.getBeamSchema()))
        .apply("Beam Rows into BQ Rows", ParDo.of(new DoFn<Row, TableRow>() {

          private static final long serialVersionUID = 1L;

          @ProcessElement
          public void processElement(ProcessContext c) {
            String[] columnNames = schemaLoad.getHeaderAsStringArray();
            TableRow row = new TableRow();
            Row beamrow = c.element();
            for (int j = 0; j < columnNames.length; j++) {
              // No typy conversion at the moment.
              row.set(columnNames[j], beamrow.getValue(columnNames[j]));
            }

            c.output(row);
          }
        })).apply("Write into BigQuery",
        BigQueryIO.writeTableRows().to(tableRef).withSchema(schemaLoad.getBQSchema())
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(isStreaming ? BigQueryIO.Write.WriteDisposition.WRITE_APPEND
                : BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE));
    try {
      logger.info("Starting pipeline");
      p.run().waitUntilFinish();
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
    }
  }
}