package io.logbase.cakebeedelivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by logbase on 30/11/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public String TrackDefault;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public int TrackDefaultFreq;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public int TrackOrderFreq;
}
