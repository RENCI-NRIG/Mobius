package org.renci.mobius.controllers.moc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.collect.MinMaxPriorityQueue;

import java.util.*;

/*
 * @brief class representing algorithm to determine flavor for storage or compute nodes
 *
 * @author kthare10
 */
public class MocFlavorAlgo {
    private static final Logger LOGGER = LogManager.getLogger(MocFlavorAlgo.class.getName());

    private static MinMaxPriorityQueue<Flavor> flavorsSortedByNumberOfCpus;
    private static Multimap<Integer, Flavor> flavorsByCpuKey;
    public static Flavor GpuP100 = new Flavor(54, 1024, 112640, "gpu.P100");
    static {
        Flavor M1Tiny = new Flavor(1, 10240, 1024, "m1.tiny");
        Flavor M1S2Tiny = new Flavor(1, 25600, 1024, "m1.s2.tiny");

        Flavor M1Small = new Flavor(1, 10240, 2048, "m1.small");
        Flavor M1S2Small = new Flavor(1, 25600, 2048, "m1.s2.small");

        Flavor M1Medium = new Flavor(2, 10240, 4096, "m1.medium");
        Flavor M1S2Medium = new Flavor(2, 25600, 4096, "m1.s2.medium");

        Flavor M1Large = new Flavor(4, 10240, 8192, "m1.large");
        Flavor M1S2Large = new Flavor(4, 25600, 8192, "m1.s2.large");

        Flavor M1XLarge = new Flavor(8, 10240, 16384, "m1.xlarge");
        Flavor M1S2XLarge = new Flavor(8, 25600, 16384, "m1.s2.xlarge");

        Flavor Custom4C16G = new Flavor(4, 10240, 16384, "custom.4c.16g");
        Flavor Custom4C32G = new Flavor(4, 10240, 32768, "custom.4c.32g");

        Flavor Custom8C32G = new Flavor(8, 10240, 32768, "custom.8c.32g");
        Flavor Custom8C64G = new Flavor(8, 10240, 65536, "custom.8c.64g");

        Flavor C1XLarge = new Flavor(10, 10240, 46080, "c1.xlarge");
        Flavor C1S2XLarge = new Flavor(10, 25600, 46080, "c1.s2.xlarge");

        Flavor C12XLarge = new Flavor(20, 10240, 92160, "c1.2xlarge");
        Flavor C1S22XLarge = new Flavor(20, 25600, 92160, "c1.s2.2xlarge");

        Flavor C14XLarge = new Flavor(40, 10240, 184320, "c1.4xlarge");
        Flavor C1S24XLarge = new Flavor(40, 25600, 184320, "c1.s2.4xlarge");

        flavorsSortedByNumberOfCpus = MinMaxPriorityQueue.create();
        flavorsSortedByNumberOfCpus.add(M1Tiny);
        flavorsSortedByNumberOfCpus.add(M1S2Tiny);
        flavorsSortedByNumberOfCpus.add(M1Small);
        flavorsSortedByNumberOfCpus.add(M1S2Small);
        flavorsSortedByNumberOfCpus.add(M1Medium);
        flavorsSortedByNumberOfCpus.add(M1S2Medium);
        flavorsSortedByNumberOfCpus.add(M1Large);
        flavorsSortedByNumberOfCpus.add(M1S2Large);
        flavorsSortedByNumberOfCpus.add(M1XLarge);
        flavorsSortedByNumberOfCpus.add(M1S2XLarge);

        flavorsSortedByNumberOfCpus.add(Custom4C16G);
        flavorsSortedByNumberOfCpus.add(Custom4C32G);
        flavorsSortedByNumberOfCpus.add(Custom8C32G);
        flavorsSortedByNumberOfCpus.add(Custom8C64G);

        flavorsSortedByNumberOfCpus.add(C1XLarge);
        flavorsSortedByNumberOfCpus.add(C1S2XLarge);
        flavorsSortedByNumberOfCpus.add(C12XLarge);
        flavorsSortedByNumberOfCpus.add(C1S22XLarge);
        flavorsSortedByNumberOfCpus.add(C14XLarge);
        flavorsSortedByNumberOfCpus.add(C1S24XLarge);

        flavorsByCpuKey = ArrayListMultimap.create();
        flavorsByCpuKey.put(M1Tiny.getCpus(), M1Tiny);
        flavorsByCpuKey.put(M1S2Tiny.getCpus(), M1S2Tiny);
        flavorsByCpuKey.put(M1Small.getCpus(), M1Small);
        flavorsByCpuKey.put(M1S2Small.getCpus(), M1S2Small);
        flavorsByCpuKey.put(M1Medium.getCpus(), M1Medium);
        flavorsByCpuKey.put(M1S2Medium.getCpus(), M1S2Medium);
        flavorsByCpuKey.put(M1Large.getCpus(), M1Large);
        flavorsByCpuKey.put(M1S2Large.getCpus(), M1S2Large);
        flavorsByCpuKey.put(M1XLarge.getCpus(), M1XLarge);
        flavorsByCpuKey.put(M1S2XLarge.getCpus(), M1S2XLarge);

        flavorsByCpuKey.put(Custom4C16G.getCpus(), Custom4C16G);
        flavorsByCpuKey.put(Custom4C32G.getCpus(), Custom4C32G);
        flavorsByCpuKey.put(Custom8C32G.getCpus(), Custom8C32G);
        flavorsByCpuKey.put(Custom8C64G.getCpus(), Custom8C64G);
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

    public static Map<String, Integer> determineFlavors(Integer cpus, Integer gpus, Integer ramPerCpus, Integer diskPerCpus) {
        Map<String, Integer> result = new HashMap<>();
        LOGGER.debug("IN cpus=" + cpus + " gpus=" + gpus + " ramPerCpus=" + ramPerCpus + " diskPerCpus=" + diskPerCpus);


        Collection<Flavor> flavors = flavorsByCpuKey.get(cpus);
        if(flavors != null) {
            for(Flavor flavor : flavors) {
                if(flavor.getRamPerCpu() >= ramPerCpus && flavor.getDiskPerCpu() >= diskPerCpus) {
                    result.put(flavor.getName(), 1);
                    return result;
                }
            }
        }
        Stack<Flavor> flavorStack = new Stack<>();
        boolean flag = false;

        LOGGER.debug("Size=" + flavorsSortedByNumberOfCpus.size());
        while (cpus != 0 && flavorsSortedByNumberOfCpus.size()!= 0 ) {
            Flavor flavor = flavorsSortedByNumberOfCpus.pollLast();
            LOGGER.debug("Flavor = " + flavor.getName());
            if(flavor != null) {
                int count = cpus / flavor.getCpus();
                LOGGER.debug("Count = " + count);

                int leftCpus = cpus % flavor.getCpus();
                LOGGER.debug("leftCpus = " + leftCpus);

                if (count >= 1) {
                    result.put(flavor.getName(), count);
                    if (leftCpus == 0) {
                        flavorStack.push(flavor);
                        flag = true;
                        break;
                    } else {
                        cpus -= (count * flavor.getCpus());
                    }
                }
                flavorStack.push(flavor);
            }
        }
        while (flavorStack.size()!= 0) {
            Flavor flavor = flavorStack.pop();
            if(flavor != null) {
                flavorsSortedByNumberOfCpus.add(flavor);
            }
        }
        while (gpus > 0) {
            int count = gpus / GpuP100.getCpus();
            if(count > 0) {
                count++;
            }
            result.put(GpuP100.getName(), count);
        }
        LOGGER.debug("Size=" + flavorsSortedByNumberOfCpus.size());
        if(!flag && (result.size() == 0 || cpus != 0)) {
            return null;
        }

        LOGGER.debug("OUT result=" + result.toString());
        return result;
    }
}

