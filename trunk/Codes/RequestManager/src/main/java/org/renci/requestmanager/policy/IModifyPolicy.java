/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.policy;

import org.apache.log4j.Logger;
import org.renci.requestmanager.ModifyRequestInfo;

/**
 *
 * @author anirban
 */
public interface IModifyPolicy {
      
    public int determineChangeInNumWorkers(ModifyRequestInfo modReq, String manifest);
    
}
