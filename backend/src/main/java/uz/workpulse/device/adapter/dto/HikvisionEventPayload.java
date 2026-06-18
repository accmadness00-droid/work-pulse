package uz.workpulse.device.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HikvisionEventPayload(
        @JsonProperty("AcsEvent") AcsEvent acsEvent
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AcsEvent(
            @JsonProperty("InfoList") List<Info> infoList,
            @JsonProperty("numOfMatches") Integer numOfMatches
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Info(
            @JsonProperty("serialNo") Long serialNo,
            @JsonProperty("employeeNoString") String employeeNoString,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("time") String time,
            @JsonProperty("major") Integer major,
            @JsonProperty("minor") Integer minor,
            @JsonProperty("currentVerifyMode") String currentVerifyMode
    ) {
    }
}
