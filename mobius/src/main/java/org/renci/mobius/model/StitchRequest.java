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
 * StitchRequest
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-05-09T09:08:59.381-04:00[America/New_York]")

public class StitchRequest   {
  @JsonProperty("target")
  private String target = null;

  @JsonProperty("portUrl")
  private String portUrl = null;

  @JsonProperty("tag")
  private String tag = null;

  @JsonProperty("stitchIP")
  private String stitchIP = null;

  @JsonProperty("bandwidth")
  private String bandwidth = null;

  public StitchRequest target(String target) {
    this.target = target;
    return this;
  }

  /**
   * hostname or ip of the destination node which should be stitched to
   * @return target
  **/
  @ApiModelProperty(required = true, value = "hostname or ip of the destination node which should be stitched to")
  @NotNull


  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public StitchRequest portUrl(String portUrl) {
    this.portUrl = portUrl;
    return this;
  }

  /**
   * port url for the stitch port
   * @return portUrl
  **/
  @ApiModelProperty(required = true, value = "port url for the stitch port")
  @NotNull


  public String getPortUrl() {
    return portUrl;
  }

  public void setPortUrl(String portUrl) {
    this.portUrl = portUrl;
  }

  public StitchRequest tag(String tag) {
    this.tag = tag;
    return this;
  }

  /**
   * vlan tag for the stitch port
   * @return tag
  **/
  @ApiModelProperty(required = true, value = "vlan tag for the stitch port")
  @NotNull


  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public StitchRequest stitchIP(String stitchIP) {
    this.stitchIP = stitchIP;
    return this;
  }

  /**
   * IP address for the StitchPort interface on VM
   * @return stitchIP
  **/
  @ApiModelProperty(value = "IP address for the StitchPort interface on VM")


  public String getStitchIP() {
    return stitchIP;
  }

  public void setStitchIP(String stitchIP) {
    this.stitchIP = stitchIP;
  }

  public StitchRequest bandwidth(String bandwidth) {
    this.bandwidth = bandwidth;
    return this;
  }

  /**
   * Bandwidth for the StitchPort in bps
   * @return bandwidth
  **/
  @ApiModelProperty(value = "Bandwidth for the StitchPort in bps")


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
    StitchRequest stitchRequest = (StitchRequest) o;
    return Objects.equals(this.target, stitchRequest.target) &&
        Objects.equals(this.portUrl, stitchRequest.portUrl) &&
        Objects.equals(this.tag, stitchRequest.tag) &&
        Objects.equals(this.stitchIP, stitchRequest.stitchIP) &&
        Objects.equals(this.bandwidth, stitchRequest.bandwidth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target, portUrl, tag, stitchIP, bandwidth);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StitchRequest {\n");
    
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
    sb.append("    portUrl: ").append(toIndentedString(portUrl)).append("\n");
    sb.append("    tag: ").append(toIndentedString(tag)).append("\n");
    sb.append("    stitchIP: ").append(toIndentedString(stitchIP)).append("\n");
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

