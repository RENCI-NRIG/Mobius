package org.renci.mobius.controllers.exogeni;

enum Flavor {
    Small(1,11264, 1024, "XO Small"),
    Medium(1, 26624, 3072, "XO Medium"),
    Large(2, 52224, 6144, "XO Large"),
    ExtraLarge(4, 76800, 12288, "XO Extra Large"),
    BareMetal(20, 49152, 102400, "ExoGENI Bare-Metal");

    private Flavor(Integer cpus, Integer diskSpace, Integer ram, String name) {
        this.cpus =cpus;
        this.diskSpace = diskSpace;
        this.ram = ram;
        this.name = name;
    }

    public Integer getCpus() { return cpus; }
    public Integer getDiskSpace() { return diskSpace; }
    public Integer getRam() { return ram; }
    public Integer getRamPerCpu() { return ram/cpus; }
    public Integer getDiskPerCpu() { return diskSpace/cpus; }
    public String getName() { return name; }

    private Integer cpus;
    private Integer diskSpace;
    private Integer ram;
    private String name;
}