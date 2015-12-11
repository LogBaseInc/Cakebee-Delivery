package io.logbase.cakebeedelivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by logbase on 20/11/15.
 */
public class OrderDetails {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Id;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Address;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Name;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Time;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public Integer Amount;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Mobile;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Pickedon;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Pickedat;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Deliveredon;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Deliveredat;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public List<ItemDetails> Items;
}


