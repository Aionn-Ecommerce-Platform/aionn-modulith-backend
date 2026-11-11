package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.ProductQuestionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductQuestionJdbcAdapter implements ProductQuestionPersistencePort {

    private static final RowMapper<Question> MAPPER = ProductQuestionJdbcAdapter::map;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Question save(Question question) {
        jdbcTemplate.update("""
                INSERT INTO product_questions
                    (question_id, product_id, user_id, question, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, question.questionId(), question.productId(), question.userId(),
                question.question(), Timestamp.from(question.createdAt()),
                Timestamp.from(question.updatedAt()));
        return question;
    }

    @Override
    public Optional<Question> findById(String questionId) {
        return jdbcTemplate.query("""
                SELECT * FROM product_questions WHERE question_id = ?
                """, MAPPER, questionId).stream().findFirst();
    }

    @Override
    public List<Question> findByProduct(String productId, int offset, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM product_questions
                 WHERE product_id = ?
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
                """, MAPPER, productId, limit, offset);
    }

    @Override
    public long countByProduct(String productId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_questions WHERE product_id = ?",
                Long.class, productId);
        return count == null ? 0L : count;
    }

    @Override
    public List<Question> findByMerchant(String merchantId, Boolean answered, int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT q.* FROM product_questions q
                  JOIN products p ON p.product_id = q.product_id
                 WHERE p.merchant_id = ?
                """);
        if (Boolean.TRUE.equals(answered)) {
            sql.append(" AND q.answer IS NOT NULL");
        } else if (Boolean.FALSE.equals(answered)) {
            sql.append(" AND q.answer IS NULL");
        }
        sql.append(" ORDER BY q.created_at DESC LIMIT ? OFFSET ?");
        return jdbcTemplate.query(sql.toString(), MAPPER, merchantId, limit, offset);
    }

    @Override
    public long countByMerchant(String merchantId, Boolean answered) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM product_questions q
                  JOIN products p ON p.product_id = q.product_id
                 WHERE p.merchant_id = ?
                """);
        if (Boolean.TRUE.equals(answered)) {
            sql.append(" AND q.answer IS NOT NULL");
        } else if (Boolean.FALSE.equals(answered)) {
            sql.append(" AND q.answer IS NULL");
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, merchantId);
        return count == null ? 0L : count;
    }

    @Override
    public boolean answer(String questionId, String merchantId, String answer, Instant answeredAt) {
        return jdbcTemplate.update("""
                UPDATE product_questions q
                   SET answer = ?, answered_by = ?, answered_at = ?, updated_at = ?
                 WHERE q.question_id = ?
                   AND EXISTS (
                       SELECT 1 FROM products p
                        WHERE p.product_id = q.product_id AND p.merchant_id = ?)
                """, answer, merchantId, Timestamp.from(answeredAt), Timestamp.from(answeredAt),
                questionId, merchantId) > 0;
    }

    @Override
    public void delete(String questionId, String userId) {
        jdbcTemplate.update(
                "DELETE FROM product_questions WHERE question_id = ? AND user_id = ?",
                questionId, userId);
    }

    private static Question map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp answeredAt = rs.getTimestamp("answered_at");
        return new Question(
                rs.getString("question_id"),
                rs.getString("product_id"),
                rs.getString("user_id"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getString("answered_by"),
                answeredAt == null ? null : answeredAt.toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
