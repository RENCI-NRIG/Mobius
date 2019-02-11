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
 * ComputeRequest
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-01-25T09:04:06.535-05:00[America/New_York]")

public class ComputeRequest   {
  @JsonProperty("site")
  private String site = null;

  @JsonProperty("cpus")
  private Integer cpus = null;

  @JsonProperty("gpus")
  private Integer gpus = null;

  @JsonProperty("ramPerCpus")
  private Integer ramPerCpus = null;

  @JsonProperty("diskPerCpus")
  private Integer diskPerCpus = null;

  @JsonProperty("leaseStart")
  private String leaseStart = null;

  @JsonProperty("leaseEnd")
  private String leaseEnd = null;

  @JsonProperty("coallocate")
  private Boolean coallocate = false;

  /**
   * Indicates Slice policy to be used. For 'new' slicePolicy, compute resources are added in a new slice on site specified. For 'existing' slicePolicy, compute resources are added to existing slice specified by 'sliceName' field. For 'default' slicePolicy, compute resources are either added to an existing slice with same leaseEndTime if found or added to a new slice on site specified. Default value is 'default'
   */
  public enum SlicePolicyEnum {
    NEW("new"),
    
    EXISTING("existing"),
    
    DEFAULT("default");

    private String value;

    SlicePolicyEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SlicePolicyEnum fromValue(String text) {
      for (SlicePolicyEnum b : SlicePolicyEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("slicePolicy")
  private SlicePolicyEnum slicePolicy = SlicePolicyEnum.DEFAULT;

  @JsonProperty("sliceName")
  private String sliceName = null;

  @JsonProperty("imageUrl")
  private String imageUrl = null;

  @JsonProperty("imageHash")
  private String imageHash = null;

  @JsonProperty("imageName")
  private String imageName = null;

  @JsonProperty("postBootScript")
  private String postBootScript = null;

  public ComputeRequest site(String site) {
    this.site = site;
    return this;
  }

  /**
   * execution site
   * @return site
  **/
  @ApiModelProperty(required = true, value = "execution site")
  @NotNull


  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public ComputeRequest cpus(Integer cpus) {
    this.cpus = cpus;
    return this;
  }

  /**
   * number of cpus
   * minimum: 0
   * @return cpus
  **/
  @ApiModelProperty(required = true, value = "number of cpus")
  @NotNull

@Min(0)
  public Integer getCpus() {
    return cpus;
  }

  public void setCpus(Integer cpus) {
    this.cpus = cpus;
  }

  public ComputeRequest gpus(Integer gpus) {
    this.gpus = gpus;
    return this;
  }

  /**
   * number of gpus
   * minimum: 0
   * @return gpus
  **/
  @ApiModelProperty(required = true, value = "number of gpus")
  @NotNull

@Min(0)
  public Integer getGpus() {
    return gpus;
  }

  public void setGpus(Integer gpus) {
    this.gpus = gpus;
  }

  public ComputeRequest ramPerCpus(Integer ramPerCpus) {
    this.ramPerCpus = ramPerCpus;
    return this;
  }

  /**
   * RAM per cpu in MB
   * minimum: 0
   * @return ramPerCpus
  **/
  @ApiModelProperty(required = true, value = "RAM per cpu in MB")
  @NotNull

@Min(0)
  public Integer getRamPerCpus() {
    return ramPerCpus;
  }

  public void setRamPerCpus(Integer ramPerCpus) {
    this.ramPerCpus = ramPerCpus;
  }

  public ComputeRequest diskPerCpus(Integer diskPerCpus) {
    this.diskPerCpus = diskPerCpus;
    return this;
  }

  /**
   * Disk per cpu in MB
   * minimum: 0
   * @return diskPerCpus
  **/
  @ApiModelProperty(required = true, value = "Disk per cpu in MB")
  @NotNull

@Min(0)
  public Integer getDiskPerCpus() {
    return diskPerCpus;
  }

  public void setDiskPerCpus(Integer diskPerCpus) {
    this.diskPerCpus = diskPerCpus;
  }

  public ComputeRequest leaseStart(String leaseStart) {
    this.leaseStart = leaseStart;
    return this;
  }

  /**
   * Least Start Time
   * @return leaseStart
  **/
  @ApiModelProperty(required = true, value = "Least Start Time")
  @NotNull


  public String getLeaseStart() {
    return leaseStart;
  }

  public void setLeaseStart(String leaseStart) {
    this.leaseStart = leaseStart;
  }

  public ComputeRequest leaseEnd(String leaseEnd) {
    this.leaseEnd = leaseEnd;
    return this;
  }

  /**
   * Least End Time
   * @return leaseEnd
  **/
  @ApiModelProperty(required = true, value = "Least End Time")
  @NotNull


  public String getLeaseEnd() {
    return leaseEnd;
  }

  public void setLeaseEnd(String leaseEnd) {
    this.leaseEnd = leaseEnd;
  }

  public ComputeRequest coallocate(Boolean coallocate) {
    this.coallocate = coallocate;
    return this;
  }

  /**
   * flag indicating if CPUs should be allocated across multiple compute resources or not. Should be set to 'true' if CPUs should be coallocated on single compute resource. Default value is 'false'
   * @return coallocate
  **/
  @ApiModelProperty(required = true, value = "flag indicating if CPUs should be allocated across multiple compute resources or not. Should be set to 'true' if CPUs should be coallocated on single compute resource. Default value is 'false'")
  @NotNull


  public Boolean isCoallocate() {
    return coallocate;
  }

  public void setCoallocate(Boolean coallocate) {
    this.coallocate = coallocate;
  }

  public ComputeRequest slicePolicy(SlicePolicyEnum slicePolicy) {
    this.slicePolicy = slicePolicy;
    return this;
  }

  /**
   * Indicates Slice policy to be used. For 'new' slicePolicy, compute resources are added in a new slice on site specified. For 'existing' slicePolicy, compute resources are added to existing slice specified by 'sliceName' field. For 'default' slicePolicy, compute resources are either added to an existing slice with same leaseEndTime if found or added to a new slice on site specified. Default value is 'default'
   * @return slicePolicy
  **/
  @ApiModelProperty(required = true, value = "Indicates Slice policy to be used. For 'new' slicePolicy, compute resources are added in a new slice on site specified. For 'existing' slicePolicy, compute resources are added to existing slice specified by 'sliceName' field. For 'default' slicePolicy, compute resources are either added to an existing slice with same leaseEndTime if found or added to a new slice on site specified. Default value is 'default'")
  @NotNull


  public SlicePolicyEnum getSlicePolicy() {
    return slicePolicy;
  }

  public void setSlicePolicy(SlicePolicyEnum slicePolicy) {
    this.slicePolicy = slicePolicy;
  }

  public ComputeRequest sliceName(String sliceName) {
    this.sliceName = sliceName;
    return this;
  }

  /**
   * existing slice name to which compute resources should be added
   * @return sliceName
  **/
  @ApiModelProperty(value = "existing slice name to which compute resources should be added")


  public String getSliceName() {
    return sliceName;
  }

  public void setSliceName(String sliceName) {
    this.sliceName = sliceName;
  }

  public ComputeRequest imageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }

  /**
   * image url
   * @return imageUrl
  **/
  @ApiModelProperty(value = "image url")


  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public ComputeRequest imageHash(String imageHash) {
    this.imageHash = imageHash;
    return this;
  }

  /**
   * image hash
   * @return imageHash
  **/
  @ApiModelProperty(value = "image hash")


  public String getImageHash() {
    return imageHash;
  }

  public void setImageHash(String imageHash) {
    this.imageHash = imageHash;
  }

  public ComputeRequest imageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

  /**
   * image name
   * @return imageName
  **/
  @ApiModelProperty(value = "image name")


  public String getImageName() {
    return imageName;
  }

  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  public ComputeRequest postBootScript(String postBootScript) {
    this.postBootScript = postBootScript;
    return this;
  }

  /**
   * post boot script
   * @return postBootScript
  **/
  @ApiModelProperty(value = "post boot script")


  public String getPostBootScript() {
    return postBootScript;
  }

  public void setPostBootScript(String postBootScript) {
    this.postBootScript = postBootScript;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComputeRequest computeRequest = (ComputeRequest) o;
    return Objects.equals(this.site, computeRequest.site) &&
        Objects.equals(this.cpus, computeRequest.cpus) &&
        Objects.equals(this.gpus, computeRequest.gpus) &&
        Objects.equals(this.ramPerCpus, computeRequest.ramPerCpus) &&
        Objects.equals(this.diskPerCpus, computeRequest.diskPerCpus) &&
        Objects.equals(this.leaseStart, computeRequest.leaseStart) &&
        Objects.equals(this.leaseEnd, computeRequest.leaseEnd) &&
        Objects.equals(this.coallocate, computeRequest.coallocate) &&
        Objects.equals(this.slicePolicy, computeRequest.slicePolicy) &&
        Objects.equals(this.sliceName, computeRequest.sliceName) &&
        Objects.equals(this.imageUrl, computeRequest.imageUrl) &&
        Objects.equals(this.imageHash, computeRequest.imageHash) &&
        Objects.equals(this.imageName, computeRequest.imageName) &&
        Objects.equals(this.postBootScript, computeRequest.postBootScript);
  }

  @Override
  public int hashCode() {
    return Objects.hash(site, cpus, gpus, ramPerCpus, diskPerCpus, leaseStart, leaseEnd, coallocate, slicePolicy, sliceName, imageUrl, imageHash, imageName, postBootScript);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComputeRequest {\n");
    
    sb.append("    site: ").append(toIndentedString(site)).append("\n");
    sb.append("    cpus: ").append(toIndentedString(cpus)).append("\n");
    sb.append("    gpus: ").append(toIndentedString(gpus)).append("\n");
    sb.append("    ramPerCpus: ").append(toIndentedString(ramPerCpus)).append("\n");
    sb.append("    diskPerCpus: ").append(toIndentedString(diskPerCpus)).append("\n");
    sb.append("    leaseStart: ").append(toIndentedString(leaseStart)).append("\n");
    sb.append("    leaseEnd: ").append(toIndentedString(leaseEnd)).append("\n");
    sb.append("    coallocate: ").append(toIndentedString(coallocate)).append("\n");
    sb.append("    slicePolicy: ").append(toIndentedString(slicePolicy)).append("\n");
    sb.append("    sliceName: ").append(toIndentedString(sliceName)).append("\n");
    sb.append("    imageUrl: ").append(toIndentedString(imageUrl)).append("\n");
    sb.append("    imageHash: ").append(toIndentedString(imageHash)).append("\n");
    sb.append("    imageName: ").append(toIndentedString(imageName)).append("\n");
    sb.append("    postBootScript: ").append(toIndentedString(postBootScript)).append("\n");
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

