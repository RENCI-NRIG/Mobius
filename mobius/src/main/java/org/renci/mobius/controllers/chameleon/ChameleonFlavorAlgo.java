package org.renci.mobius.controllers.chameleon;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;

import java.util.*;

public class ChameleonFlavorAlgo {
    private static final Logger LOGGER = Logger.getLogger(ChameleonFlavorAlgo.class.getName());

    enum Flavor {
        // cpus, diskspace, ram, name
        Skylake(48, 240057, 196608, "compute_skylake"),
        Haswell(48, 250059, 131072, "compute_haswell"),
        InfiniBand(48, 250059, 131072, "compute_haswell_ib"),
        Gpus(56, 250059, 131072, "XO Extra Large");

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

    public static Map<String, Integer> determineFlavors(Integer cpus, Integer ramPerCpus, Integer diskPerCpus,
                                                        boolean isCoallocate) {
        Map<String, Integer> result = new HashMap<>();
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
                        int value = 1;
                        if(result.containsKey(f.getName())) {
                            value = result.get(f.getName());
                            ++value;
                        }

                        result.put(f.getName(), value);
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
                        int value = 1;
                        if(result.containsKey(Flavor.Skylake.getName())) {
                            value = result.get(Flavor.Skylake.getName());
                            ++value;
                        }

                        result.put(Flavor.Skylake.getName(), value);
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.Haswell.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.Haswell.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.Haswell.getCpus(); i = i + Flavor.Haswell.getCpus()) {
                        int value = 1;
                        if(result.containsKey(Flavor.Haswell.getName())) {
                            value = result.get(Flavor.Haswell.getName());
                            ++value;
                        }

                        result.put(Flavor.Haswell.getName(), value);
                    }
                    requestedCpus -= i;
                }
            }
            if (requestedCpus != 0 && ramPerCpus <= Flavor.InfiniBand.getRamPerCpu()) {
                if (diskPerCpus <= Flavor.InfiniBand.getDiskPerCpu()) {
                    for (i = 0; i < requestedCpus / Flavor.InfiniBand.getCpus(); i = i + Flavor.InfiniBand.getCpus()) {
                        int value = 1;
                        if(result.containsKey(Flavor.InfiniBand.getName())) {
                            value = result.get(Flavor.InfiniBand.getName());
                            ++value;
                        }
                        
                        result.put(Flavor.InfiniBand.getName(), value);
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
