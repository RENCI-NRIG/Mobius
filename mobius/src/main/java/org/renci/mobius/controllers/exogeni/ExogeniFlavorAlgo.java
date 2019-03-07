package org.renci.mobius.controllers.exogeni;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class ExogeniFlavorAlgo {
    private static final Logger LOGGER = Logger.getLogger( ExogeniFlavorAlgo.class.getName() );

    enum Flavor {
        // cpus, diskspace, ram, name
        Small(1, 11264, 1024, "XO Small"),
        Medium(1, 26624, 3072, "XO Medium"),
        Large(2, 52224, 6144, "XO Large"),
        ExtraLarge(4, 76800, 12288, "XO Extra Large"),
        BareMetal(20, 49152, 102400, "ExoGENI Bare-Metal");

        private Flavor(Integer cpus, Integer diskSpace, Integer ram, String name) {
            this.cpus = cpus;
            this.diskSpace = diskSpace;
            this.ram = ram;
            this.name = name;
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
    public static final Multimap<Integer, Flavor> cpusToFlavorMap;
    static {
        Multimap<Integer, Flavor> fv = ArrayListMultimap.create();
        fv.put(Flavor.Small.getCpus(), Flavor.Small);
        fv.put(Flavor.Medium.getCpus(), Flavor.Medium);
        fv.put(Flavor.Large.getCpus(), Flavor.Large);
        fv.put(Flavor.ExtraLarge.getCpus(), Flavor.ExtraLarge);
        fv.put(Flavor.BareMetal.getCpus(), Flavor.BareMetal);
        cpusToFlavorMap = fv;
    }
    public static final Multimap<Integer, Flavor> diskToFlavorMap;
    static {
        Multimap<Integer, Flavor> fv = ArrayListMultimap.create();
        fv.put(Flavor.Small.getDiskPerCpu(), Flavor.Small);
        fv.put(Flavor.Medium.getDiskPerCpu(), Flavor.Medium);
        fv.put(Flavor.Large.getDiskPerCpu(), Flavor.Large);
        fv.put(Flavor.ExtraLarge.getDiskPerCpu(), Flavor.ExtraLarge);
        fv.put(Flavor.BareMetal.getDiskPerCpu(), Flavor.BareMetal);
        diskToFlavorMap = fv;
    }
    public static final Multimap<Integer, Flavor> ramToFlavorMap;
    static {
        Multimap<Integer, Flavor> fv = ArrayListMultimap.create();
        fv.put(Flavor.Small.getRamPerCpu(), Flavor.Small);
        fv.put(Flavor.Medium.getRamPerCpu(), Flavor.Medium);
        fv.put(Flavor.Large.getRamPerCpu(), Flavor.Large);
        fv.put(Flavor.ExtraLarge.getRamPerCpu(), Flavor.ExtraLarge);
        fv.put(Flavor.BareMetal.getRamPerCpu(), Flavor.BareMetal);
        ramToFlavorMap = fv;
    }
    public static List<String> determineFlavors(Integer cpus, Integer ramPerCpus, Integer diskPerCpus, boolean isCoallocate) {
        LOGGER.debug("determineFlavors: IN");

        List<String> result = new LinkedList<String>();
        Integer requestedCpus = cpus;

        if(isCoallocate) {
            Collection<Flavor> flavors = null;
            // Number of CPUs matches a flavor
            if(cpusToFlavorMap.containsKey(requestedCpus)){
                flavors = cpusToFlavorMap.get(requestedCpus);
            }
            // Find flavor which can accommodate cpus
            else {
                for(Integer cpu : cpusToFlavorMap.keySet()) {
                    if(cpus <= cpu) {
                        flavors = cpusToFlavorMap.get(cpu);
                        break;
                    }
                }
            }
            if(flavors != null) {
                for (Flavor f : flavors) {
                    if (diskPerCpus <= f.getDiskPerCpu() && ramPerCpus <= f.getRamPerCpu()) {
                        result.add(f.getName());
                        requestedCpus = 0;
                    }
                }
            }
        }
        else {
            int i = 0;
            if (requestedCpus != 0 && ramPerCpus <= Flavor.Small.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.Small.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.Small.getCpus(); i = i + Flavor.Small.getCpus()) {
                        result.add(Flavor.Small.getName());
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.Medium.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.Medium.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.Medium.getCpus(); i = i + Flavor.Medium.getCpus()) {
                        result.add(Flavor.Medium.getName());
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.Large.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.Large.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.Large.getCpus(); i = i + Flavor.Large.getCpus()) {
                        result.add(Flavor.Large.getName());
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.ExtraLarge.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.ExtraLarge.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.ExtraLarge.getCpus(); i = i + Flavor.ExtraLarge.getCpus()) {
                        result.add(Flavor.ExtraLarge.getName());
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.BareMetal.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.BareMetal.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.BareMetal.getCpus(); i = i + Flavor.BareMetal.getCpus()) {
                        result.add(Flavor.BareMetal.getName());
                    }
                    requestedCpus -= i;
                }
            }
        }
        if(requestedCpus != 0) {
            result = null;
        }
        LOGGER.debug("determineFlavors: OUT");
        return result;
    }
}