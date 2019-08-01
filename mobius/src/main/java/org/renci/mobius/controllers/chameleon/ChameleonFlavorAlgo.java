package org.renci.mobius.controllers.chameleon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.renci.mobius.controllers.MobiusException;
import org.springframework.http.HttpStatus;

import java.util.*;

/*
 * @brief class representing algorithm to determine flavor for storage or compute nodes
 *
 * @author kthare10
 */
public class ChameleonFlavorAlgo {
    private static final Logger LOGGER = LogManager.getLogger(ChameleonFlavorAlgo.class.getName());

    private static final Integer minCpus = Flavor.Haswell.getCpus();

    enum Flavor {
        // cpus, diskspace, ram, name
        Haswell(48, 250059, 131072, "compute_haswell"),
        InfiniBand(48, 250059, 131072, "compute_haswell_ib"),
        Skylake(48, 240057, 196608, "compute_skylake"),
        Gpus(56, 250059, 131072, "gpu_k80, gpu_m40, gpu_p100, gpu_p100_nvlink"),
        Storage(40, 30000000, 64000, "storage");

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

    public static final List<Flavor> computeflavors;
    static {
        List<Flavor> fv = new ArrayList<>();
        fv.add(Flavor.Haswell);
        fv.add(Flavor.InfiniBand);
        fv.add(Flavor.Skylake);
        computeflavors = fv;
    }


    /*
     * @brief Determine the number of storage instances to satisfy the request
     *
     * @param size - Disk space requested in GBs
     *
     * @return Returns a map of flavor name to number of instances required for the flavor
     *
     */
    public static Map<String, Integer> determineFlavors(Integer size) {
        Map<String, Integer> result = new HashMap<>();

        int count = ((1000 * size)/ Flavor.Storage.getDiskSpace());
        if((1000 * size) % (Flavor.Storage.getDiskSpace()) != 0) {
            count++;
        }
        result.put(Flavor.Storage.getName(), count);
        return result;
    }

    /*
     * @brief Determine the number of vm instances for each flavor required to satisfy the request
     *
     * @param cpus - Number of cpus requested
     * @param gpus - Number of gpus requested
     * @prarm ramPerCpus - Ram per cpu in MBs requested
     * @param diskPerCpus - Disk space per cpu in MBs requested
     *
     * @return Returns a map of flavor name to number of instances required for the flavor
     *
     */

    public static Map<String, Integer> determineFlavors(Integer cpus, Integer gpus, Integer ramPerCpus,
                                                        Integer diskPerCpus, String forceFlavor) throws Exception{
        Map<String, Integer> result = new HashMap<>();
        Integer requestedCpus = cpus;
        LOGGER.debug("IN: cpus=" + cpus + " gpus=" + gpus + " ramPerCpus=" + ramPerCpus + " diskPerCpus=" + diskPerCpus + " forceFlavor=" + forceFlavor);

        // Number of CPUs, DiskSpace and RAM for Haswell, Skylake and InfinityBand nodes is almost similar
        // Current algorithm; just picks a flavor in a round robin fashion between these 3 compute flavors
        if(cpus != 0) {
            int count = cpus / minCpus;
            if ((cpus % minCpus) != 0) {
                count++;
            }

            if(forceFlavor != null) {
                if ( forceFlavor.compareToIgnoreCase(Flavor.Haswell.name) == 0) {
                    result.put(Flavor.Haswell.name, count);
                }
                else if (forceFlavor.compareToIgnoreCase(Flavor.InfiniBand.name) == 0 ) {
                    result.put(Flavor.InfiniBand.name, count);
                }
                else if (forceFlavor.compareToIgnoreCase(Flavor.Skylake.name) == 0 ) {
                    result.put(Flavor.Skylake.name, count);
                }
                else {
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid flavor specified in forceflavor");
                }
            }
            else {
                LOGGER.debug("computeflavors before shuffle: " + computeflavors);
                Collections.shuffle(computeflavors);
                LOGGER.debug("computeflavors after shuffle: " + computeflavors);
                result.put(computeflavors.get(0).getName(), count);

                if (result.isEmpty()) {
                    return null;
                }
            }
        }

        if(gpus != 0) {
            int count = (gpus/ Flavor.Gpus.getCpus());
            if(gpus % Flavor.Gpus.getCpus() != 0) {
                count++;
            }

            String flavorNames = Flavor.Gpus.getName();
            List<String> gpuflavors = Arrays.asList(flavorNames.split(","));
            LOGGER.debug("gpuflavors before shuffle: " + gpuflavors);
            Collections.shuffle(gpuflavors);
            LOGGER.debug("gpuflavors after shuffle: " + gpuflavors);
            result.put(gpuflavors.get(0), count);

            if (result.isEmpty()) {
                return null;
            }
        }


        LOGGER.debug("OUT result=" + result);
        return result;
    }
}
