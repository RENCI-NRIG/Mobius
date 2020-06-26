package org.renci.mobius.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * SdxPrefix
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T11:35:33.989-04:00[America/New_York]")
public class SdxPrefix   {
  @JsonProperty("source")
  private String source = null;

  @JsonProperty("sourceSubnet")
  private String sourceSubnet = null;

  @JsonProperty("gatewayIP")
  private String gatewayIP = null;

  @JsonProperty("destinationSubnet")
  private String destinationSubnet = null;

  @JsonProperty("bandwidth")
  private String bandwidth = null;

  public SdxPrefix source(String source) {
    this.source = source;
    return this;
  }

  /**
   * Source Node Host Name
   * @return source
  **/
  @ApiModelProperty(required = true, value = "Source Node Host Name")
      @NotNull

    public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public SdxPrefix sourceSubnet(String sourceSubnet) {
    this.sourceSubnet = sourceSubnet;
    return this;
  }

  /**
   * Source Subnet
   * @return sourceSubnet
  **/
  @ApiModelProperty(required = true, value = "Source Subnet")
      @NotNull

    public String getSourceSubnet() {
    return sourceSubnet;
  }

  public void setSourceSubnet(String sourceSubnet) {
    this.sourceSubnet = sourceSubnet;
  }

  public SdxPrefix gatewayIP(String gatewayIP) {
    this.gatewayIP = gatewayIP;
    return this;
  }

  /**
   * Gateway IP Address
   * @return gatewayIP
  **/
  @ApiModelProperty(required = true, value = "Gateway IP Address")
      @NotNull

    public String getGatewayIP() {
    return gatewayIP;
  }

  public void setGatewayIP(String gatewayIP) {
    this.gatewayIP = gatewayIP;
  }

  public SdxPrefix destinationSubnet(String destinationSubnet) {
    this.destinationSubnet = destinationSubnet;
    return this;
  }

  /**
   * Destination Subnet
   * @return destinationSubnet
  **/
  @ApiModelProperty(value = "Destination Subnet")
  
    public String getDestinationSubnet() {
    return destinationSubnet;
  }

  public void setDestinationSubnet(String destinationSubnet) {
    this.destinationSubnet = destinationSubnet;
  }

  public SdxPrefix bandwidth(String bandwidth) {
    this.bandwidth = bandwidth;
    return this;
  }

  /**
   * Bandwidth
   * @return bandwidth
  **/
  @ApiModelProperty(value = "Bandwidth")
  
    public String getBandwidth() {
    return bandwidth;
  }

  public void setBandwidth(String bandwidth) {
    this.bandwidth = bandwidth;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SdxPrefix sdxPrefix = (SdxPrefix) o;
    return Objects.equals(this.source, sdxPrefix.source) &&
        Objects.equals(this.sourceSubnet, sdxPrefix.sourceSubnet) &&
        Objects.equals(this.gatewayIP, sdxPrefix.gatewayIP) &&
        Objects.equals(this.destinationSubnet, sdxPrefix.destinationSubnet) &&
        Objects.equals(this.bandwidth, sdxPrefix.bandwidth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, sourceSubnet, gatewayIP, destinationSubnet, bandwidth);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SdxPrefix {\n");
    
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    sourceSubnet: ").append(toIndentedString(sourceSubnet)).append("\n");
    sb.append("    gatewayIP: ").append(toIndentedString(gatewayIP)).append("\n");
    sb.append("    destinationSubnet: ").append(toIndentedString(destinationSubnet)).append("\n");
    sb.append("    bandwidth: ").append(toIndentedString(bandwidth)).append("\n");
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
