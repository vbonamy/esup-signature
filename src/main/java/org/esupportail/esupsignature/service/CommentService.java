package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

@Service
public class CommentService {

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @Transactional
    public Comment create(Long signRequestId, String text, Integer posX, Integer posY, Integer pageNumer, Integer stepNumber, Boolean postit, String postitColor, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignRequest signRequest = signRequestService.getById(signRequestId);
        Comment comment = new Comment();
        comment.setText(text);
        comment.setCreateBy(user);
        comment.setCreateDate(new Date());
        comment.setPosX(posX);
        comment.setPosY(posY);
        comment.setPageNumber(pageNumer);
        comment.setStepNumber(stepNumber);
        comment.setPostit(postit);
        if(postitColor != null) {
            comment.setPostitColor(postitColor);
        }
        commentRepository.save(comment);
        signRequest.getComments().add(comment);
        return comment;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if(comment.isPresent()) {
            SignRequest signRequest = signRequestService.getSignRequestByComment(comment.get());
            if (comment.get().getStepNumber() != null) {
                signRequest.getSignRequestParams().remove(comment.get().getStepNumber() - 1);
                if(signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().size() > comment.get().getStepNumber()) {
                    signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(comment.get().getStepNumber() - 1).setSignRequestParams(null);
                }
            }
            signRequest.getComments().remove(comment.get());
            commentRepository.delete(comment.get());
        }
    }

}
