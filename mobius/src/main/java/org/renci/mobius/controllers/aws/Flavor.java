package org.renci.mobius.controllers.aws;

import com.amazonaws.services.ec2.model.InstanceType;

public class Flavor implements Comparable<Flavor> {
    // cpus, diskspace(MB), ram(MB), name
    public Flavor(Integer cpus, Integer gpus, Integer diskSpace, Integer ram, Integer gpuram, InstanceType type) {
        this.cpus = cpus;
        this.gpus = gpus;
        this.diskSpace = diskSpace;
        this.ram = ram;
        this.gpuram = gpuram;
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Flavor that) {
        return (cpus - that.getCpus());
    }

    public Integer getCpus() {
        return cpus;
    }

    public Integer getGpus() {
        return gpus;
    }

    public Integer getDiskSpace() {
        return diskSpace;
    }

    public Integer getRam() {
        return ram;
    }

    public Integer getRamPerCpu() {
        return ram / cpus;
    }

    public Integer getGpuRam() {
        return gpuram;
    }

    public Integer getGpuRamPerGpu() {
        return gpuram / gpus;
    }

    public Integer getDiskPerCpu() {
        return diskSpace / cpus;
    }


    public InstanceType getType() { return type; }

    private Integer cpus;
    private Integer gpus;
    private Integer diskSpace;
    private Integer ram;
    private Integer gpuram;
    private InstanceType type;
}
