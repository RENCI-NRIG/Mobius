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
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-04-14T10:07:05.150-04:00[America/New_York]")
public class NetworkRequest   {
  @JsonProperty("source")
  private String source = null;

  @JsonProperty("sourceIP")
  private String sourceIP = null;

  @JsonProperty("sourceSubnet")
  private String sourceSubnet = null;

  @JsonProperty("sourceLocalSubnet")
  private String sourceLocalSubnet = null;

  @JsonProperty("destination")
  private String destination = null;

  @JsonProperty("destinationIP")
  private String destinationIP = null;

  @JsonProperty("destinationSubnet")
  private String destinationSubnet = null;

  @JsonProperty("destLocalSubnet")
  private String destLocalSubnet = null;

  @JsonProperty("linkSpeed")
  private String linkSpeed = "1000000000";

  @JsonProperty("leaseStart")
  private String leaseStart = null;

  @JsonProperty("leaseEnd")
  private String leaseEnd = null;

  @JsonProperty("chameleonSdxControllerIP")
  private String chameleonSdxControllerIP = null;

  /**
   * Gets or Sets action
   */
  public enum ActionEnum {
    ADD("add"),
    
    DELETE("delete");

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
   * hostname of the source node
   * @return source
  **/
  @ApiModelProperty(required = true, value = "hostname of the source node")
      @NotNull

    public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public NetworkRequest sourceIP(String sourceIP) {
    this.sourceIP = sourceIP;
    return this;
  }

  /**
   * ip of the source node
   * @return sourceIP
  **/
  @ApiModelProperty(required = true, value = "ip of the source node")
      @NotNull

    public String getSourceIP() {
    return sourceIP;
  }

  public void setSourceIP(String sourceIP) {
    this.sourceIP = sourceIP;
  }

  public NetworkRequest sourceSubnet(String sourceSubnet) {
    this.sourceSubnet = sourceSubnet;
    return this;
  }

  /**
   * subnet of the source node
   * @return sourceSubnet
  **/
  @ApiModelProperty(required = true, value = "subnet of the source node")
      @NotNull

    public String getSourceSubnet() {
    return sourceSubnet;
  }

  public void setSourceSubnet(String sourceSubnet) {
    this.sourceSubnet = sourceSubnet;
  }

  public NetworkRequest sourceLocalSubnet(String sourceLocalSubnet) {
    this.sourceLocalSubnet = sourceLocalSubnet;
    return this;
  }

  /**
   * Local subnet at the source to which traffic from source be routed
   * @return sourceLocalSubnet
  **/
  @ApiModelProperty(value = "Local subnet at the source to which traffic from source be routed")
  
    public String getSourceLocalSubnet() {
    return sourceLocalSubnet;
  }

  public void setSourceLocalSubnet(String sourceLocalSubnet) {
    this.sourceLocalSubnet = sourceLocalSubnet;
  }

  public NetworkRequest destination(String destination) {
    this.destination = destination;
    return this;
  }

  /**
   * hostname of the destination node
   * @return destination
  **/
  @ApiModelProperty(required = true, value = "hostname of the destination node")
      @NotNull

    public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public NetworkRequest destinationIP(String destinationIP) {
    this.destinationIP = destinationIP;
    return this;
  }

  /**
   * ip of the destination node
   * @return destinationIP
  **/
  @ApiModelProperty(required = true, value = "ip of the destination node")
      @NotNull

    public String getDestinationIP() {
    return destinationIP;
  }

  public void setDestinationIP(String destinationIP) {
    this.destinationIP = destinationIP;
  }

  public NetworkRequest destinationSubnet(String destinationSubnet) {
    this.destinationSubnet = destinationSubnet;
    return this;
  }

  /**
   * subnet of the destination node
   * @return destinationSubnet
  **/
  @ApiModelProperty(required = true, value = "subnet of the destination node")
      @NotNull

    public String getDestinationSubnet() {
    return destinationSubnet;
  }

  public void setDestinationSubnet(String destinationSubnet) {
    this.destinationSubnet = destinationSubnet;
  }

  public NetworkRequest destLocalSubnet(String destLocalSubnet) {
    this.destLocalSubnet = destLocalSubnet;
    return this;
  }

  /**
   * Local subnet at the destination to which traffic from source be routed
   * @return destLocalSubnet
  **/
  @ApiModelProperty(value = "Local subnet at the destination to which traffic from source be routed")
  
    public String getDestLocalSubnet() {
    return destLocalSubnet;
  }

  public void setDestLocalSubnet(String destLocalSubnet) {
    this.destLocalSubnet = destLocalSubnet;
  }

  public NetworkRequest linkSpeed(String linkSpeed) {
    this.linkSpeed = linkSpeed;
    return this;
  }

  /**
   * Link speed
   * @return linkSpeed
  **/
  @ApiModelProperty(value = "Link speed")
  
    public String getLinkSpeed() {
    return linkSpeed;
  }

  public void setLinkSpeed(String linkSpeed) {
    this.linkSpeed = linkSpeed;
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

  public NetworkRequest chameleonSdxControllerIP(String chameleonSdxControllerIP) {
    this.chameleonSdxControllerIP = chameleonSdxControllerIP;
    return this;
  }

  /**
   * IP address used by SDX controller for the new interface to Stitchport
   * @return chameleonSdxControllerIP
  **/
  @ApiModelProperty(value = "IP address used by SDX controller for the new interface to Stitchport")
  
    public String getChameleonSdxControllerIP() {
    return chameleonSdxControllerIP;
  }

  public void setChameleonSdxControllerIP(String chameleonSdxControllerIP) {
    this.chameleonSdxControllerIP = chameleonSdxControllerIP;
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
    return Objects.equals(this.source, networkRequest.source) &&
        Objects.equals(this.sourceIP, networkRequest.sourceIP) &&
        Objects.equals(this.sourceSubnet, networkRequest.sourceSubnet) &&
        Objects.equals(this.sourceLocalSubnet, networkRequest.sourceLocalSubnet) &&
        Objects.equals(this.destination, networkRequest.destination) &&
        Objects.equals(this.destinationIP, networkRequest.destinationIP) &&
        Objects.equals(this.destinationSubnet, networkRequest.destinationSubnet) &&
        Objects.equals(this.destLocalSubnet, networkRequest.destLocalSubnet) &&
        Objects.equals(this.linkSpeed, networkRequest.linkSpeed) &&
        Objects.equals(this.leaseStart, networkRequest.leaseStart) &&
        Objects.equals(this.leaseEnd, networkRequest.leaseEnd) &&
        Objects.equals(this.chameleonSdxControllerIP, networkRequest.chameleonSdxControllerIP) &&
        Objects.equals(this.action, networkRequest.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, sourceIP, sourceSubnet, sourceLocalSubnet, destination, destinationIP, destinationSubnet, destLocalSubnet, linkSpeed, leaseStart, leaseEnd, chameleonSdxControllerIP, action);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NetworkRequest {\n");
    
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    sourceIP: ").append(toIndentedString(sourceIP)).append("\n");
    sb.append("    sourceSubnet: ").append(toIndentedString(sourceSubnet)).append("\n");
    sb.append("    sourceLocalSubnet: ").append(toIndentedString(sourceLocalSubnet)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    destinationIP: ").append(toIndentedString(destinationIP)).append("\n");
    sb.append("    destinationSubnet: ").append(toIndentedString(destinationSubnet)).append("\n");
    sb.append("    destLocalSubnet: ").append(toIndentedString(destLocalSubnet)).append("\n");
    sb.append("    linkSpeed: ").append(toIndentedString(linkSpeed)).append("\n");
    sb.append("    leaseStart: ").append(toIndentedString(leaseStart)).append("\n");
    sb.append("    leaseEnd: ").append(toIndentedString(leaseEnd)).append("\n");
    sb.append("    chameleonSdxControllerIP: ").append(toIndentedString(chameleonSdxControllerIP)).append("\n");
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
