package org.renci.mobius.controllers.jetstream;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jdk.nashorn.internal.ir.WhileNode;
import org.apache.log4j.Logger;
import com.google.common.collect.MinMaxPriorityQueue;

import java.util.*;

/*
 * @brief class representing algorithm to determine flavor for storage or compute nodes
 *
 * @author kthare10
 */
public class JetstreamFlavorAlgo {
    private static final Logger LOGGER = Logger.getLogger(JetstreamFlavorAlgo.class.getName());

    private static MinMaxPriorityQueue<Flavor> flavorsSortedByNumberOfCpus;
    private static Multimap<Integer, Flavor> flavorsByCpuKey;
    static {
        Flavor M1Tiny = new Flavor(1, 8192, 2048, "m1.tiny");
        Flavor M1Small = new Flavor(2, 20480, 4096, "m1.small");
        Flavor M1Quad = new Flavor(4, 20480, 10240, "m1.quad");
        Flavor M1Medium = new Flavor(6, 61440, 16384, "m1.medium");
        Flavor M1Large = new Flavor(10, 61440, 30720, "m1.large");
        Flavor S1Large = new Flavor(10, 245760, 30720, "s1.large");
        Flavor M1XLarge = new Flavor(24, 61440, 61440, "m1.xlarge");
        Flavor S1XLarge = new Flavor(24, 491520, 61440, "s1.xlarge");
        Flavor M1XXLarge = new Flavor(44, 61440, 122880, "m1.xxlarge");
        Flavor S1XXLarge = new Flavor(44, 983040, 122880, "s1.xxlarge");

        flavorsSortedByNumberOfCpus = MinMaxPriorityQueue.create();
        flavorsSortedByNumberOfCpus.add(M1Tiny);
        flavorsSortedByNumberOfCpus.add(M1Small);
        flavorsSortedByNumberOfCpus.add(M1Quad);
        flavorsSortedByNumberOfCpus.add(M1Medium);
        flavorsSortedByNumberOfCpus.add(M1Large);
        flavorsSortedByNumberOfCpus.add(S1Large);
        flavorsSortedByNumberOfCpus.add(M1XLarge);
        flavorsSortedByNumberOfCpus.add(S1XLarge);
        flavorsSortedByNumberOfCpus.add(M1XXLarge);
        flavorsSortedByNumberOfCpus.add(S1XXLarge);

        flavorsByCpuKey = ArrayListMultimap.create();
        flavorsByCpuKey.put(M1Tiny.getCpus(), M1Tiny);
        flavorsByCpuKey.put(M1Small.getCpus(), M1Small);
        flavorsByCpuKey.put(M1Quad.getCpus(), M1Quad);
        flavorsByCpuKey.put(M1Medium.getCpus(), M1Medium);
        flavorsByCpuKey.put(M1Large.getCpus(), M1Large);
        flavorsByCpuKey.put(S1Large.getCpus(), S1Large);
        flavorsByCpuKey.put(M1XLarge.getCpus(), M1XLarge);
        flavorsByCpuKey.put(S1XLarge.getCpus(), S1XLarge);
        flavorsByCpuKey.put(M1XXLarge.getCpus(), M1XXLarge);
        flavorsByCpuKey.put(S1XXLarge.getCpus(), S1XXLarge);
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

    public static Map<String, Integer> determineFlavors(Integer cpus, Integer ramPerCpus, Integer diskPerCpus) {
        Map<String, Integer> result = new HashMap<>();
        LOGGER.debug("determineFlavors: IN");


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
        LOGGER.debug("Size=" + flavorsSortedByNumberOfCpus.size());
        if(!flag && (result.size() == 0 || cpus != 0)) {
            return null;
        }

        LOGGER.debug("determineFlavors: OUT");
        return result;
    }
}
