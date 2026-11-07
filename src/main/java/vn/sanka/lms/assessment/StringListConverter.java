package vn.sanka.lms.assessment;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>,String> {
    private static final ObjectMapper JSON=new ObjectMapper();
    public String convertToDatabaseColumn(List<String> values){
        try{return JSON.writeValueAsString(values);}catch(Exception e){throw new IllegalStateException("Cannot store exam choices",e);}
    }
    public List<String> convertToEntityAttribute(String value){
        try{return JSON.readValue(value,new TypeReference<List<String>>(){});}catch(Exception e){throw new IllegalStateException("Cannot read exam choices",e);}
    }
}
