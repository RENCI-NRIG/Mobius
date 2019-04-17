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
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-04-10T14:01:54.953-04:00[America/New_York]")

public class StitchRequest   {
  @JsonProperty("target")
  private String target = null;

  @JsonProperty("portUrl")
  private String portUrl = null;

  @JsonProperty("tag")
  private String tag = null;

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
        Objects.equals(this.tag, stitchRequest.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target, portUrl, tag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StitchRequest {\n");
    
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
    sb.append("    portUrl: ").append(toIndentedString(portUrl)).append("\n");
    sb.append("    tag: ").append(toIndentedString(tag)).append("\n");
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

