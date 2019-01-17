package org.renci.mobius.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * NetworkRequest
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2018-12-12T15:18:29.707-05:00[America/New_York]")

public class NetworkRequest   {
  @JsonProperty("source")
  private String source = null;

  @JsonProperty("destination")
  private String destination = null;

  @JsonProperty("linkSpeed")
  private Integer linkSpeed = null;

  @JsonProperty("qos")
  private String qos = null;

  @JsonProperty("leaseStart")
  private String leaseStart = null;

  @JsonProperty("leaseEnd")
  private String leaseEnd = null;

  /**
   * Gets or Sets action
   */
  public enum ActionEnum {
    ADD("add"),
    
    DELETE("delete"),
    
    UPDATE("update");

    private String value;

    ActionEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ActionEnum fromValue(String text) {
      for (ActionEnum b : ActionEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("action")
  private ActionEnum action = null;

  public NetworkRequest source(String source) {
    this.source = source;
    return this;
  }

  /**
   * hostname or ip of the source
   * @return source
  **/
  @ApiModelProperty(value = "hostname or ip of the source")


  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public NetworkRequest destination(String destination) {
    this.destination = destination;
    return this;
  }

  /**
   * hostname or ip of the destination
   * @return destination
  **/
  @ApiModelProperty(value = "hostname or ip of the destination")


  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public NetworkRequest linkSpeed(Integer linkSpeed) {
    this.linkSpeed = linkSpeed;
    return this;
  }

  /**
   * Link speed
   * minimum: 0
   * @return linkSpeed
  **/
  @ApiModelProperty(value = "Link speed")

@Min(0)
  public Integer getLinkSpeed() {
    return linkSpeed;
  }

  public void setLinkSpeed(Integer linkSpeed) {
    this.linkSpeed = linkSpeed;
  }

  public NetworkRequest qos(String qos) {
    this.qos = qos;
    return this;
  }

  /**
   * Qos parameters
   * @return qos
  **/
  @ApiModelProperty(value = "Qos parameters")


  public String getQos() {
    return qos;
  }

  public void setQos(String qos) {
    this.qos = qos;
  }

  public NetworkRequest leaseStart(String leaseStart) {
    this.leaseStart = leaseStart;
    return this;
  }

  /**
   * Least Start Time
   * @return leaseStart
  **/
  @ApiModelProperty(value = "Least Start Time")


  public String getLeaseStart() {
    return leaseStart;
  }

  public void setLeaseStart(String leaseStart) {
    this.leaseStart = leaseStart;
  }

  public NetworkRequest leaseEnd(String leaseEnd) {
    this.leaseEnd = leaseEnd;
    return this;
  }

  /**
   * Least End Time
   * @return leaseEnd
  **/
  @ApiModelProperty(value = "Least End Time")


  public String getLeaseEnd() {
    return leaseEnd;
  }

  public void setLeaseEnd(String leaseEnd) {
    this.leaseEnd = leaseEnd;
  }

  public NetworkRequest action(ActionEnum action) {
    this.action = action;
    return this;
  }

  /**
   * Get action
   * @return action
  **/
  @ApiModelProperty(value = "")


  public ActionEnum getAction() {
    return action;
  }

  public void setAction(ActionEnum action) {
    this.action = action;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NetworkRequest networkRequest = (NetworkRequest) o;
    return Objects.equals(this.source, networkRequest.source) &&
        Objects.equals(this.destination, networkRequest.destination) &&
        Objects.equals(this.linkSpeed, networkRequest.linkSpeed) &&
        Objects.equals(this.qos, networkRequest.qos) &&
        Objects.equals(this.leaseStart, networkRequest.leaseStart) &&
        Objects.equals(this.leaseEnd, networkRequest.leaseEnd) &&
        Objects.equals(this.action, networkRequest.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination, linkSpeed, qos, leaseStart, leaseEnd, action);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NetworkRequest {\n");
    
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    linkSpeed: ").append(toIndentedString(linkSpeed)).append("\n");
    sb.append("    qos: ").append(toIndentedString(qos)).append("\n");
    sb.append("    leaseStart: ").append(toIndentedString(leaseStart)).append("\n");
    sb.append("    leaseEnd: ").append(toIndentedString(leaseEnd)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

