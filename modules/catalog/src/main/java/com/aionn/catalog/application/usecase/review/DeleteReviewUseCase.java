package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.command.DeleteReviewCommand;
import com.aionn.catalog.application.port.in.review.DeleteReviewInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteReviewUseCase implements DeleteReviewInputPort {
    private final ReviewService reviewService;

    @Override
    public void execute(DeleteReviewCommand command) {
        reviewService.delete(command);
    }
}
