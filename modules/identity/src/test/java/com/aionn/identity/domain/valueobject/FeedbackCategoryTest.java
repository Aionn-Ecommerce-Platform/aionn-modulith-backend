package com.aionn.identity.domain.valueobject;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackCategoryTest {

    @Test
    void fromBlankDefaultsToGeneral() {
        assertThat(FeedbackCategory.from(null)).isEqualTo(FeedbackCategory.GENERAL);
        assertThat(FeedbackCategory.from(" ")).isEqualTo(FeedbackCategory.GENERAL);
    }

    @Test
    void fromUnsupportedCategoryUsesFeedbackErrorCode() {
        assertThatThrownBy(() -> FeedbackCategory.from("shipping"))
                .isInstanceOfSatisfying(IdentityException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.INVALID_FEEDBACK_CATEGORY.getCode()));
    }
}
