package com.mycloud.Utilities;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileReader {

    private static Logger logger = LoggerFactory.getLogger(FileReader.class);

    public String readFile(String fileName){
        String fileContent="";
        StringBuffer path = new StringBuffer(FileReader.class.getClassLoader().getResource(".").toString().substring(5));
        String filePath = path.append(fileName).toString();
        //System.out.println(path);
        try(InputStream ins = new FileInputStream(filePath)){
            
            fileContent=IOUtils.toString(ins, "UTF-8");

        }catch(Exception e){
            e.printStackTrace();
        }
        logger.info((fileName+" File Readed Successfully"));
        return fileContent;
    }
    
}
