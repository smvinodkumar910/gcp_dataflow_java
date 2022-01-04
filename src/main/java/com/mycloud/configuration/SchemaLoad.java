package com.mycloud.configuration;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.json.JSONArray;
import org.json.JSONObject;
 
import com.google.api.services.bigquery.model.TableSchema;
import com.mycloud.Utilities.FileReader;
import com.google.api.services.bigquery.model.TableFieldSchema;


import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.Schema.Field;
import org.apache.beam.sdk.schemas.Schema.FieldType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SchemaLoad implements Serializable {

    private static final long serialVersionUID = -7259193914232579304L;

    private static Logger logger = LoggerFactory.getLogger(SchemaLoad.class);
    
    private String SchemaString;

    public SchemaLoad(String bqtableName){
        this.SchemaString = new FileReader().readFile(bqtableName+".json");

    }
    
    public JSONObject getSchemaAsJson(){
        
        JSONObject SchemaJson = new JSONObject(this.SchemaString);
        logger.info("Returning schema as Json");
        return SchemaJson;
    }
    //col1,col2,col3,
    public String getHeaderAsCsvString(){
        JSONArray fields = this.getSchemaAsJson().getJSONArray("fields");
        StringBuffer headerString= new StringBuffer();
        for(int i=0; i<fields.length() ;i++){
            if (i==0){
                headerString.append(fields.getJSONObject(i).getString("name"));
            }else{
                headerString.append(",");
                headerString.append(fields.getJSONObject(i).get("name"));
            }
            logger.info(headerString.toString());
        }
        logger.info("Returning schema as comma delimited String");
        return headerString.toString();
    }
    //[col1, col2, col3]
    public String[] getHeaderAsStringArray(){
        logger.info("Returning schema as String[]");
        return this.getHeaderAsCsvString().split(",");
    }

    public TableSchema getBQSchema() {
        List<TableFieldSchema> tableFieldSchemaList = new ArrayList<>();
        JSONArray fields = this.getSchemaAsJson().getJSONArray("fields");
        for(int i=0; i<fields.length() ;i++){
            tableFieldSchemaList.add(new TableFieldSchema()
                        .setName(fields.getJSONObject(i).getString("name"))
                        .setType(fields.getJSONObject(i).getString("type")));
        }
        logger.info("Returning schema as BigQuery Row object");
        return new TableSchema().setFields(tableFieldSchemaList);
      }

    public Schema getBeamSchema(){
        List<Field> fieldList = new ArrayList<>();
        JSONArray fields = this.getSchemaAsJson().getJSONArray("fields");
        
        for(int i=0; i<fields.length() ;i++){
            
            JSONObject field = fields.getJSONObject(i);
            fieldList.add(Field.of(field.getString("name"),deriveType(field.getString("type")) ));
        }
        Schema sc = new Schema(fieldList);
        logger.info("Returning schema as Beam Row object");
        return sc;
    }

    private FieldType deriveType(String type){
        switch(type){
            case "STRING":
                return FieldType.STRING;
            case "INTEGER":
                return FieldType.INT16 ;
            default:
                return FieldType.STRING;
        }
    }


    
    public static void main(String[] args) {

        SchemaLoad schemaLoad = new SchemaLoad("Load_API_Data");
        logger.info(schemaLoad.getHeaderAsCsvString());
        
    }
    
}
