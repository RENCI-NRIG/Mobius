package org.renci.mobius.controllers;

import org.renci.mobius.controllers.aws.AwsContext;
import org.renci.mobius.controllers.chameleon.ChameleonContext;
import org.renci.mobius.controllers.exogeni.ExogeniContext;
import org.renci.mobius.controllers.jetstream.JetstreamContext;
import org.renci.mobius.controllers.mos.MosContext;
import org.springframework.http.HttpStatus;

/*
 * @brief class implements factory to instantiate different kinds of contexts based on cloud type
 *
 * @author kthare10
 */
public class CloudContextFactory {
    private static final CloudContextFactory fINSTANCE = new CloudContextFactory();

    /*
     * @brief constructor
     */
    private CloudContextFactory() {}

    /*
     * @brief returns factory instance
     *
     * @return factory instance
     */
    public static CloudContextFactory getInstance() {
        return fINSTANCE;
    }

    /*
     * @brief create cloud context
     *
     * @param site - site
     * @param workflowId - workflowId
     *
     * @return cloud context
     *
     * @throws exception in case of error
     */
    public CloudContext createCloudContext(String site, String workflowId) throws Exception{
        if(site.contains(CloudContext.CloudType.Chameleon.toString()) == true) {
            ChameleonContext chameleonContext = new ChameleonContext(CloudContext.CloudType.Chameleon, site, workflowId);
            return chameleonContext;
        }
        else if(site.contains(CloudContext.CloudType.Aws.toString()) == true) {
            return new AwsContext(CloudContext.CloudType.Aws, site, workflowId);
        }
        else if(site.contains(CloudContext.CloudType.Mos.toString()) == true) {
            return new MosContext(CloudContext.CloudType.Mos, site, workflowId);
        }
        else if(site.contains(CloudContext.CloudType.Exogeni.toString()) == true) {
            return new ExogeniContext(CloudContext.CloudType.Exogeni, site, workflowId);
        }
        else if(site.contains(CloudContext.CloudType.Jetstream.toString()) == true) {
            return new JetstreamContext(CloudContext.CloudType.Jetstream, site, workflowId);
        }
        throw new MobiusException(HttpStatus.BAD_REQUEST, "Unsupported cloud type=" + site);
    }
}
