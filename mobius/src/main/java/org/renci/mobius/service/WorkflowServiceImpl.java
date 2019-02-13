package org.renci.mobius.service;

import org.renci.mobius.entity.WorkflowEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.renci.mobius.repository.WorkflowRepository;

import java.util.List;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Override
    public WorkflowEntity createWorkflow(WorkflowEntity workflow) {
        return workflowRepository.save(workflow);
    }

    @Override
    public WorkflowEntity getWorkflow(String id) {
        return workflowRepository.findOne(id);
    }

    @Override
    public WorkflowEntity editWorkflow(WorkflowEntity workflow) {
        return workflowRepository.save(workflow);
    }

    @Override
    public void deleteWorkflow(WorkflowEntity workflow) {
        workflowRepository.delete(workflow);
    }

    @Override
    public void deleteWorkflow(String id) {
        workflowRepository.delete(id);
    }

    @Override
    public List<WorkflowEntity> getAllWorkflows(int pageNumber, int pageSize) {
        return workflowRepository.findAll(new PageRequest(pageNumber, pageSize)).getContent();
    }

    @Override
    public List<WorkflowEntity> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    @Override
    public long countWorkflows() {
        return workflowRepository.count();
    }
}

