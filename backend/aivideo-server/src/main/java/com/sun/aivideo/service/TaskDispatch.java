package com.sun.aivideo.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/** 确保任务和凭证版本提交成功后，后台线程才读取任务。 */
@Component
public class TaskDispatch {
    private final TransactionTemplate rejectionTransaction;
    public TaskDispatch(PlatformTransactionManager manager) {
        rejectionTransaction = new TransactionTemplate(manager);
        rejectionTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public void afterCommit(Runnable action, Runnable rejected) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { action.run(); }
                catch (TaskRejectedException e) {
                    rejectionTransaction.executeWithoutResult(status -> rejected.run());
                }
            }
        });
    }
}
