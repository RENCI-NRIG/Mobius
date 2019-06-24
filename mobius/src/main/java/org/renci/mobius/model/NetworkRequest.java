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
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-06-21T14:54:33.975-04:00[America/New_York]")

public class NetworkRequest   {
  @JsonProperty("sourceIP")
  private String sourceIP = null;

  @JsonProperty("sourceSliceName")
  private String sourceSliceName = null;

  @JsonProperty("destinationIP")
  private String destinationIP = null;

  @JsonProperty("destinationSliceName")
  private String destinationSliceName = null;

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

  public NetworkRequest sourceIP(String sourceIP) {
    this.sourceIP = sourceIP;
    return this;
  }

  /**
   * ip of the source
   * @return sourceIP
  **/
  @ApiModelProperty(required = true, value = "ip of the source")
  @NotNull


  public String getSourceIP() {
    return sourceIP;
  }

  public void setSourceIP(String sourceIP) {
    this.sourceIP = sourceIP;
  }

  public NetworkRequest sourceSliceName(String sourceSliceName) {
    this.sourceSliceName = sourceSliceName;
    return this;
  }

  /**
   * ip of the source
   * @return sourceSliceName
  **/
  @ApiModelProperty(required = true, value = "ip of the source")
  @NotNull


  public String getSourceSliceName() {
    return sourceSliceName;
  }

  public void setSourceSliceName(String sourceSliceName) {
    this.sourceSliceName = sourceSliceName;
  }

  public NetworkRequest destinationIP(String destinationIP) {
    this.destinationIP = destinationIP;
    return this;
  }

  /**
   * ip of the destination
   * @return destinationIP
  **/
  @ApiModelProperty(required = true, value = "ip of the destination")
  @NotNull


  public String getDestinationIP() {
    return destinationIP;
  }

  public void setDestinationIP(String destinationIP) {
    this.destinationIP = destinationIP;
  }

  public NetworkRequest destinationSliceName(String destinationSliceName) {
    this.destinationSliceName = destinationSliceName;
    return this;
  }

  /**
   * ip of the destination
   * @return destinationSliceName
  **/
  @ApiModelProperty(required = true, value = "ip of the destination")
  @NotNull


  public String getDestinationSliceName() {
    return destinationSliceName;
  }

  public void setDestinationSliceName(String destinationSliceName) {
    this.destinationSliceName = destinationSliceName;
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
  @ApiModelProperty(required = true, value = "")
  @NotNull


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
    return Objects.equals(this.sourceIP, networkRequest.sourceIP) &&
        Objects.equals(this.sourceSliceName, networkRequest.sourceSliceName) &&
        Objects.equals(this.destinationIP, networkRequest.destinationIP) &&
        Objects.equals(this.destinationSliceName, networkRequest.destinationSliceName) &&
        Objects.equals(this.linkSpeed, networkRequest.linkSpeed) &&
        Objects.equals(this.qos, networkRequest.qos) &&
        Objects.equals(this.leaseStart, networkRequest.leaseStart) &&
        Objects.equals(this.leaseEnd, networkRequest.leaseEnd) &&
        Objects.equals(this.action, networkRequest.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceIP, sourceSliceName, destinationIP, destinationSliceName, linkSpeed, qos, leaseStart, leaseEnd, action);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NetworkRequest {\n");
    
    sb.append("    sourceIP: ").append(toIndentedString(sourceIP)).append("\n");
    sb.append("    sourceSliceName: ").append(toIndentedString(sourceSliceName)).append("\n");
    sb.append("    destinationIP: ").append(toIndentedString(destinationIP)).append("\n");
    sb.append("    destinationSliceName: ").append(toIndentedString(destinationSliceName)).append("\n");
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

