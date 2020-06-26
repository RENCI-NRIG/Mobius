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
 * ScriptRequest
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-25T14:22:01.560-04:00[America/New_York]")
public class ScriptRequest   {
  @JsonProperty("target")
  private String target = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("script")
  private String script = null;

  public ScriptRequest target(String target) {
    this.target = target;
    return this;
  }

  /**
   * Target Node Host Name
   * @return target
  **/
  @ApiModelProperty(required = true, value = "Target Node Host Name")
      @NotNull

    public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public ScriptRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Script Name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "Script Name")
      @NotNull

    public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ScriptRequest script(String script) {
    this.script = script;
    return this;
  }

  /**
   * Script to be executed
   * @return script
  **/
  @ApiModelProperty(required = true, value = "Script to be executed")
      @NotNull

    public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScriptRequest scriptRequest = (ScriptRequest) o;
    return Objects.equals(this.target, scriptRequest.target) &&
        Objects.equals(this.name, scriptRequest.name) &&
        Objects.equals(this.script, scriptRequest.script);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target, name, script);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScriptRequest {\n");
    
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    script: ").append(toIndentedString(script)).append("\n");
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
