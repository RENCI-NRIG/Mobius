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
 * ComputeRequest
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2018-12-17T08:25:38.605-05:00[America/New_York]")

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

  @JsonProperty("imageUrl")
  private String imageUrl = null;

  @JsonProperty("imageHash")
  private String imageHash = null;

  @JsonProperty("imageName")
  private String imageName = null;

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
        Objects.equals(this.imageUrl, computeRequest.imageUrl) &&
        Objects.equals(this.imageHash, computeRequest.imageHash) &&
        Objects.equals(this.imageName, computeRequest.imageName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(site, cpus, gpus, ramPerCpus, diskPerCpus, leaseStart, leaseEnd, imageUrl, imageHash, imageName);
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
    sb.append("    imageUrl: ").append(toIndentedString(imageUrl)).append("\n");
    sb.append("    imageHash: ").append(toIndentedString(imageHash)).append("\n");
    sb.append("    imageName: ").append(toIndentedString(imageName)).append("\n");
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

