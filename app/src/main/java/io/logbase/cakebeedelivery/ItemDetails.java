package io.logbase.cakebeedelivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by logbase on 30/11/15.
 */
public class ItemDetails {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Name;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String Description;
}
