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
 * MobiusResponse
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2018-12-12T15:18:29.707-05:00[America/New_York]")

public class MobiusResponse   {
  @JsonProperty("status")
  private Integer status = null;

  @JsonProperty("message")
  private String message = null;

  @JsonProperty("value")
  private Object value = null;

  @JsonProperty("version")
  private String version = null;

  public MobiusResponse status(Integer status) {
    this.status = status;
    return this;
  }

  /**
   * status code
   * minimum: 0
   * @return status
  **/
  @ApiModelProperty(value = "status code")

@Min(0)
  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public MobiusResponse message(String message) {
    this.message = message;
    return this;
  }

  /**
   * status message
   * @return message
  **/
  @ApiModelProperty(value = "status message")


  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public MobiusResponse value(Object value) {
    this.value = value;
    return this;
  }

  /**
   * JSON object
   * @return value
  **/
  @ApiModelProperty(value = "JSON object")


  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public MobiusResponse version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Mobius version
   * @return version
  **/
  @ApiModelProperty(value = "Mobius version")


  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MobiusResponse mobiusResponse = (MobiusResponse) o;
    return Objects.equals(this.status, mobiusResponse.status) &&
        Objects.equals(this.message, mobiusResponse.message) &&
        Objects.equals(this.value, mobiusResponse.value) &&
        Objects.equals(this.version, mobiusResponse.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, message, value, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MobiusResponse {\n");
    
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

