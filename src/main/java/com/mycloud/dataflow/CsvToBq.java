
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


import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.mycloud.configuration.AppProperties;
import com.mycloud.configuration.SchemaLoad;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.Description;
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

public class CsvToBq {

  private static Logger logger = LoggerFactory.getLogger(ApiToBq.class);


  public interface CsvToBqOptions extends PipelineOptions {
    @Description("Source table name")
    String getSourceTableName();

    void setSourceTableName(String value);
  }

  

  public static void main(String[] args) {

    CsvToBqOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(CsvToBqOptions.class) ;
    
    // Read application properties
    AppProperties appProperties = new AppProperties();

    // Get BQ Table details
    Map<String, String> tabledtl = appProperties.getProperty(options.getSourceTableName());

    //Get Data schema properties
    SchemaLoad schemaLoad = new SchemaLoad(tabledtl.get("tableid"));

    // Currently hard-code the variables, this can be passed into as parameters
    // String tempLocationPath = "gs://mycloud-proj-bucket/staging";
    boolean isStreaming = true;
    TableReference tableRef = new TableReference();
    // Replace this with your own GCP project id
    
    tableRef.setProjectId(tabledtl.get("projectid"));
    tableRef.setDatasetId(tabledtl.get("datasetid"));
    tableRef.setTableId(tabledtl.get("tableid"));

    
    
    // This is required for BigQuery
    options.setTempLocation(tabledtl.get("tempLocationPath"));
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
    options.setJobName("csvtobq-"+timeStamp);

    Pipeline p = Pipeline.create(options);
    logger.info("Creating pipeline");
    p.apply("Read CSV Data", TextIO.read().from(tabledtl.get("sourcefilepath")) )
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
      p.run();
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
    }
  }
}