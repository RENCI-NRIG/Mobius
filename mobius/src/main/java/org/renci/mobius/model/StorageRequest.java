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
 * StorageRequest
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2018-12-12T15:18:29.707-05:00[America/New_York]")

public class StorageRequest   {
  @JsonProperty("mountPoint")
  private String mountPoint = null;

  @JsonProperty("target")
  private String target = null;

  @JsonProperty("size")
  private Integer size = null;

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

  public StorageRequest mountPoint(String mountPoint) {
    this.mountPoint = mountPoint;
    return this;
  }

  /**
   * mount point
   * @return mountPoint
  **/
  @ApiModelProperty(value = "mount point")


  public String getMountPoint() {
    return mountPoint;
  }

  public void setMountPoint(String mountPoint) {
    this.mountPoint = mountPoint;
  }

  public StorageRequest target(String target) {
    this.target = target;
    return this;
  }

  /**
   * hostname or ip of the destination
   * @return target
  **/
  @ApiModelProperty(value = "hostname or ip of the destination")


  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public StorageRequest size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * size in bytes
   * minimum: 0
   * @return size
  **/
  @ApiModelProperty(value = "size in bytes")

@Min(0)
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public StorageRequest leaseStart(String leaseStart) {
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

  public StorageRequest leaseEnd(String leaseEnd) {
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

  public StorageRequest action(ActionEnum action) {
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
    StorageRequest storageRequest = (StorageRequest) o;
    return Objects.equals(this.mountPoint, storageRequest.mountPoint) &&
        Objects.equals(this.target, storageRequest.target) &&
        Objects.equals(this.size, storageRequest.size) &&
        Objects.equals(this.leaseStart, storageRequest.leaseStart) &&
        Objects.equals(this.leaseEnd, storageRequest.leaseEnd) &&
        Objects.equals(this.action, storageRequest.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mountPoint, target, size, leaseStart, leaseEnd, action);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StorageRequest {\n");
    
    sb.append("    mountPoint: ").append(toIndentedString(mountPoint)).append("\n");
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
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

