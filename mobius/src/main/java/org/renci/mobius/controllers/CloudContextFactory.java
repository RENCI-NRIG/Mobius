package org.renci.mobius.controllers;

import org.renci.mobius.controllers.exogeni.ExogeniContext;
import org.springframework.http.HttpStatus;

public class CloudContextFactory {
    private static final CloudContextFactory fINSTANCE = new CloudContextFactory();

    private CloudContextFactory() {}

    public static CloudContextFactory getInstance() {
        return fINSTANCE;
    }

    public CloudContext createCloudContext(String site) throws Exception{
        if(site.compareToIgnoreCase(CloudContext.CloudType.Chameleon.toString()) == 0) {
            throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not Implemented");
        }
        else if(site.compareToIgnoreCase(CloudContext.CloudType.OSG.toString()) == 0) {
            throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not Implemented");
        }
        else if(site.contains(CloudContext.CloudType.Exogeni.toString()) == true) {
            return new ExogeniContext(CloudContext.CloudType.Exogeni, site);
        }
        throw new MobiusException(HttpStatus.BAD_REQUEST, "Unsupported cloud type=" + site);
    }
}
