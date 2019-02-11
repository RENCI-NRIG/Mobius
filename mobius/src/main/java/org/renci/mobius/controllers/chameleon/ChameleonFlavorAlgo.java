package org.renci.mobius.controllers.chameleon;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.renci.mobius.controllers.flavor.FlavorAlgo;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ChameleonFlavorAlgo  implements FlavorAlgo {
    private static final Logger LOGGER = Logger.getLogger(ChameleonFlavorAlgo.class.getName());

    enum Flavor {
        // cpus, diskspace, ram, name
        Skylake(2, 240057, 196608, "compute_skylake"),
        Haswell(2, 250059, 131072, "compute_haswell"),
        InfiniBand(2, 250059, 131072, "compute_haswell_ib"),
        Gpus(2, 250059, 131072, "XO Extra Large");

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

    public static final Multimap<Integer, ChameleonFlavorAlgo.Flavor> cpusToFlavorMap;
    static {
        Multimap<Integer, ChameleonFlavorAlgo.Flavor> fv = ArrayListMultimap.create();
        fv.put(Flavor.Skylake.getCpus(), ChameleonFlavorAlgo.Flavor.Skylake);
        fv.put(Flavor.Haswell.getCpus(), ChameleonFlavorAlgo.Flavor.Haswell);
        fv.put(Flavor.InfiniBand.getCpus(), ChameleonFlavorAlgo.Flavor.InfiniBand);
        fv.put(Flavor.Gpus.getCpus(), Flavor.Gpus);
        cpusToFlavorMap = fv;
    }

    public static List<String> determineFlavors(Integer cpus, Integer ramPerCpus, Integer diskPerCpus, boolean isCoallocate) {
        List<String> result = new LinkedList<String>();
        Integer requestedCpus = cpus;
        LOGGER.debug("determineFlavors: IN");

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
            if (requestedCpus != 0 && ramPerCpus <= Flavor.Skylake.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.Skylake.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.Skylake.getCpus(); i = i + Flavor.Skylake.getCpus()) {
                        result.add(Flavor.Skylake.getName());
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.Haswell.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.Haswell.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.Haswell.getCpus(); i = i + Flavor.Haswell.getCpus()) {
                        result.add(Flavor.Haswell.getName());
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.InfiniBand.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.InfiniBand.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.InfiniBand.getCpus(); i = i + Flavor.InfiniBand.getCpus()) {
                        result.add(Flavor.InfiniBand.getName());
                    }
                    requestedCpus -= i;
                }
            }
        }

        LOGGER.debug("determineFlavors: OUT");
        if(requestedCpus != 0) {
            result = null;
        }
        return result;
    }
}
