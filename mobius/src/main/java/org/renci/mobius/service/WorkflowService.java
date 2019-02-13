package org.renci.mobius.service;

import java.util.List;
import org.renci.mobius.entity.WorkflowEntity;

public interface WorkflowService {

    WorkflowEntity createWorkflow(WorkflowEntity workflow);

    WorkflowEntity getWorkflow(String id);

    WorkflowEntity editWorkflow(WorkflowEntity workflow);

    void deleteWorkflow(WorkflowEntity workflow);

    void deleteWorkflow(String id);

    List<WorkflowEntity> getAllWorkflows(int pageNumber, int pageSize);

    List<WorkflowEntity> getAllWorkflows();

    long countWorkflows();
}
