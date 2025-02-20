package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long> {
    List<SignBook> findByCreateByEppn(String createByEppn);
    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);
    @Query("select count(s.id) from SignBook s join s.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn and r.signed is false")
    Long countByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);
    @Query("select s from SignBook s where s.liveWorkflow.workflow.id = :workflowId")
    List<SignBook> findByWorkflowId(Long workflowId);
    @Query("select s from SignBook s where s.liveWorkflow.workflow = :workflow order by s.id")
    List<SignBook> findByLiveWorkflowWorkflow(Workflow workflow);
    @Query("select count(s) from SignBook s where s.liveWorkflow.workflow = :workflow")
    int countByLiveWorkflowWorkflow(Workflow workflow);
    List<SignBook> findByLiveWorkflowAndStatus(LiveWorkflow liveWorkflow, SignRequestStatus signRequestStatus);
    List<SignBook> findByViewersContaining(User user);
}