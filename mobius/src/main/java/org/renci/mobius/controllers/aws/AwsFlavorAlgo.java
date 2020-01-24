package org.renci.mobius.controllers.aws;

import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/*
 * @brief class representing algorithm to determine flavor for storage or compute nodes
 *
 * @author kthare10
 */
public class AwsFlavorAlgo {
    private static final Logger LOGGER = LogManager.getLogger(AwsFlavorAlgo.class.getName());

    private static MinMaxPriorityQueue<Flavor> flavorsSortedByNumberOfCpus;
    private static Multimap<Integer, Flavor> flavorsByCpuKey;
    public static Flavor P32xlarge = new Flavor(8, 1, 1024, 62464, 16384, InstanceType.P32xlarge);
    static {
        Flavor M1Tiny = new Flavor(1, 0,10240, 1024, 0, InstanceType.T2Micro);


        flavorsSortedByNumberOfCpus = MinMaxPriorityQueue.create();
        flavorsSortedByNumberOfCpus.add(M1Tiny);

        flavorsByCpuKey = ArrayListMultimap.create();
        flavorsByCpuKey.put(M1Tiny.getCpus(), M1Tiny);

    }



    /*
     * @brief Determine the number of vm instances for each flavor required to satisfy the request
     *
     * @param cpus - Number of cpus requested
     * @param gpus - Number of gpus requested
     * @prarm ramPerCpus - Ram per cpu in MBs requested
     * @param diskPerCpus - Disk space per cpu in MBs requested
     *
     * @return Returns a map of flavor to number of instances required for the flavor
     *
     */

    public static Map<InstanceType, Integer> determineFlavors(Integer cpus, Integer gpus, Integer ramPerCpus,
                                                        Integer ramPerGpus, Integer diskPerCpus) {
        Map<InstanceType, Integer> result = new HashMap<>();
        LOGGER.debug("IN cpus=" + cpus + " gpus=" + gpus + " ramPerCpus=" + ramPerCpus + " ramPerGpus="
                + ramPerGpus + " diskPerCpus=" + diskPerCpus);


        Collection<Flavor> flavors = flavorsByCpuKey.get(cpus);
        if(flavors != null) {
            for(Flavor flavor : flavors) {
                if(flavor.getRamPerCpu() >= ramPerCpus && flavor.getDiskPerCpu() >= diskPerCpus) {
                    result.put(flavor.getType(), 1);
                    return result;
                }
            }
        }
        Stack<Flavor> flavorStack = new Stack<>();
        boolean flag = false;

        LOGGER.debug("Size=" + flavorsSortedByNumberOfCpus.size());
        while (cpus != 0 && flavorsSortedByNumberOfCpus.size()!= 0 ) {
            Flavor flavor = flavorsSortedByNumberOfCpus.pollLast();
            LOGGER.debug("Flavor = " + flavor.getType());
            if(flavor != null) {
                int count = cpus / flavor.getCpus();
                LOGGER.debug("Count = " + count);

                int leftCpus = cpus % flavor.getCpus();
                LOGGER.debug("leftCpus = " + leftCpus);

                if (count >= 1) {
                    result.put(flavor.getType(), count);
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
            int count = gpus / P32xlarge.getGpus();
            if(count > 0) {
                count++;
            }
            result.put(P32xlarge.getType(), count);
        }
        LOGGER.debug("Size=" + flavorsSortedByNumberOfCpus.size());
        if(!flag && (result.size() == 0 || cpus != 0)) {
            return null;
        }

        LOGGER.debug("OUT result=" + result.toString());
        return result;
    }
}
