package org.renci.mobius.controllers.moc;

/*
 * @brief class representing flavor for storage or compute nodes
 *
 * @author kthare10
 */
public class Flavor implements Comparable<Flavor> {
    // cpus, diskspace(MB), ram(MB), name
    public Flavor(Integer cpus, Integer diskSpace, Integer ram, String name) {
        this.cpus = cpus;
        this.diskSpace = diskSpace;
        this.ram = ram;
        this.name = name;
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

    public Integer getDiskSpace() {
        return diskSpace;
    }

    public Integer getRam() {
        return ram;
    }

    public Integer getRamPerCpu() {
        return ram / cpus;
    }

    public Integer getDiskPerCpu() {
        return diskSpace / cpus;
    }

    public String getName() {
        return name;
    }

    private Integer cpus;
    private Integer diskSpace;
    private Integer ram;
    private String name;
}
